package cn.no7player.controller;

import cn.no7player.dto.ConsumerDTO;
import cn.no7player.model.Afortune;
import cn.no7player.model.HongYin;
import cn.no7player.model.OrderSign;
import cn.no7player.service.AfortuneService;
import cn.no7player.service.HongYinService;
import cn.no7player.service.OrderSignService;
import cn.no7player.util.IPUtils;
import cn.no7player.util.TimeUtils;
import cn.no7player.util.wenxin.HttpRequest;
import cn.no7player.util.wenxin.WXPayUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付JSAPI接口
 * @author wk
 * @since 2019-4-12
 * */
@Controller
@RequestMapping(value = "/pay_jsapi")
public class PayJsapiController {
    private Logger logger = LoggerFactory.getLogger(PayJsapiController.class);

    @Value("${j_appid}")
    public String appid;

    @Value("${j_appsecret}")
    public String appsecret;

    @Value("${j_mch_id}")
    public String mchId;

    @Value("${j_paternerKey}")
    public String paternerKey;

    @Value("${j_redirect_uri}")
    public String redirectUri;

    @Value("${j_createOrderURL}")
    public String createOrderURL;

    @Value("${j_notify_url}")
    public String notifyUrl;

    @Autowired
    private OrderSignService orderSignService;

    @Autowired
    private HongYinService hongYinService;

    @RequestMapping("/payWX")
    @ResponseBody
    public String payWX(String amount, String username, String gender, String birthday, String payType){
        //生成订单数据
        OrderSign orderSign = orderSignService.makeOrderHongYin(amount, username, gender, birthday, payType);

        StringBuilder sb = new StringBuilder();
        try{
            sb.append("https://open.weixin.qq.com/connect/oauth2/authorize?");
            sb.append("appid=").append(appid);
            sb.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"));
            sb.append("&response_type=code");
            sb.append("&scope=").append("snsapi_base");
            //带自己的订单参数
            sb.append("&state=").append(String.valueOf(orderSign.getId()));
            sb.append("#wechat_redirect");
        } catch (Exception e){
            e.printStackTrace();
        }
        String url = sb.toString();
        logger.info("url : {}", url);

        return url;
    }

    @RequestMapping("/pay")
    public String pay(HttpServletRequest request, String code, String state, Model model){
        model.addAttribute("code", code);
        model.addAttribute("state", state);
        logger.info("pay code = {}, state = {}", code, state);

        return "code";
    }

    /**
     * @Description 微信浏览器内微信支付/公众号支付(JSAPI)
     * @param request
     * @param code
     * @return Map
     */
    @RequestMapping(value="/payJSAPI", method = RequestMethod.GET)
    @ResponseBody
    public Map payJSAPI(HttpServletRequest request, String code, String state) {
        logger.info("payJSAPI code = {}, state = {}", code, state);
        String totalFee = "0.01";
        String outTradeNo = "";
        if(state != null && state.length() > 0){
            OrderSign orderSign = orderSignService.findById(Integer.parseInt(state));
            totalFee = String.valueOf(orderSign.getAmount().multiply(new BigDecimal(100)).intValue());
            outTradeNo = orderSign.getOrder_id();
        }

        try {
            //页面获取openId接口
            String getOpenidUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";
            String param = "appid=" + appid + "&secret=" + appsecret + "&code="+code+"&grant_type=authorization_code";
            //向微信服务器发送get请求获取openIdStr
            String openIdStr = HttpRequest.sendGet(getOpenidUrl, param);
            //转成Json格式
            JSONObject json = JSONObject.parseObject(openIdStr);
            //获取openId
            String openId = json.getString("openid");
            logger.info("openId: {}", openId);

            //拼接统一下单地址参数
            Map<String, String> paraMap = new HashMap<String, String>();
            paraMap.put("appid", appid);
            paraMap.put("body", "运势详批-姓名测算");
            paraMap.put("mch_id", mchId);
            paraMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paraMap.put("openid", openId);
            //订单号
            paraMap.put("out_trade_no", outTradeNo);
            //获取请求ip地址
            String spbillCreateIp = IPUtils.getIP(request);
            paraMap.put("spbill_create_ip", spbillCreateIp);
            paraMap.put("total_fee", totalFee);
            paraMap.put("trade_type", "JSAPI");
            // 微信服务器调用支付结果通知路径
            paraMap.put("notify_url", notifyUrl);
            String sign = WXPayUtil.generateSignature(paraMap, paternerKey);
            paraMap.put("sign", sign);
            //将所有参数(map)转xml格式
            String xml = WXPayUtil.mapToXml(paraMap);
            logger.info("xml: {}", xml);
            // 统一下单 https://api.mch.weixin.qq.com/pay/unifiedorder
            //发送post请求"统一下单接口"返回预支付id:prepay_id
            String xmlStr = HttpRequest.sendPost(createOrderURL, xml);
            logger.info("xmlStrl: {}", xmlStr);
            //以下内容是返回前端页面的json数据
            //预支付id
            String prepayId = "";
            if (xmlStr.indexOf("SUCCESS") != -1) {
                Map<String, String> map = WXPayUtil.xmlToMap(xmlStr);
                prepayId = (String) map.get("prepay_id");
            }
            Map<String, String> payMap = new HashMap<String, String>();
            payMap.put("appId", appid);
            payMap.put("timeStamp", WXPayUtil.getCurrentTimestamp()+"");
            payMap.put("nonceStr", WXPayUtil.generateNonceStr());
            payMap.put("signType", "MD5");
            payMap.put("package", "prepay_id=" + prepayId);
            String paySign = WXPayUtil.generateSignature(payMap, paternerKey);
            payMap.put("paySign", paySign);
            logger.info("prepay_id : " + prepayId);
            logger.info("paySign : " + paySign);
            return payMap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 微信支付成功,微信发送的callback信息,请注意修改订单信息
     * */
    @RequestMapping("/payCallback")
    @ResponseBody
    public String payCallback (HttpServletRequest request, HttpServletResponse response){
        logger.info("payCallback");
        InputStream is = null;
        String result = "";
        try {
            //获取请求的流信息(这里是微信发的xml格式所有只能使用流来读)
            is = request.getInputStream();
            String xml = WXPayUtil.inputStream2String(is, "UTF-8");
            //将微信发的xml转map
            Map<String, String> notifyMap = WXPayUtil.xmlToMap(xml);

            if(notifyMap.get("return_code").equals("SUCCESS")){
                if(notifyMap.get("result_code").equals("SUCCESS")){
                    //告诉微信服务器收到信息了，不要在调用回调action了========这里很重要回复微信服务器信息用流发送一个xml即可
                    result = "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>";

                    // 以下是业务处理，更新微信订单信息
                    //商户订单号
                    String outTradeNo = notifyMap.get("out_trade_no");
                    //微信支付订单号
                    String transactionId = notifyMap.get("transaction_id");
                    logger.info("outTradeNo : {}, transactionId : {}", outTradeNo, transactionId);
                    //更新订单信息
                    OrderSign orderSign = orderSignService.findByOrderId(outTradeNo);
                    if(orderSign != null){
                        orderSign.setTransaction_id(transactionId);
                        orderSignService.update(orderSign);
                        logger.info("order_id : {}, amount : {}, ifortuneId : {}",
                                orderSign.getOrder_id(), orderSign.getAmount(), orderSign.getIfortune_id());
                        //调用姓名测算
                        HongYin hongYin = hongYinService.findByOrderSignId(orderSign.getId());
                        logger.info("hongyin before: {}", hongYin);
                        hongYin = hongYinService.checkHongYinResult(hongYin);
                        logger.info("hongyin check: {}", hongYin);
                    }
                }
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
