package cn.no7player.controller;

import cn.no7player.dto.ConsumerDTO;
import cn.no7player.model.Afortune;
import cn.no7player.model.OrderSign;
import cn.no7player.service.AfortuneService;
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
    public String mch_id;

    @Value("${j_paternerKey}")
    public String paternerKey;

    @Value("${j_redirect_uri}")
    public String redirect_uri;

    @Value("${j_createOrderURL}")
    public String createOrderURL;

    @Value("${j_notify_url}")
    public String notify_url;

    @Autowired
    private AfortuneService afortuneService;

    @Autowired
    private OrderSignService orderSignService;

    @RequestMapping("/payWX")
    @ResponseBody
    public String payWX(String amount, String username, String gender, String birthday, Model model){
        Afortune afortune = new Afortune();
        afortune.setName(username);
        afortune.setFIRT_NAME(username.substring(0, 1));
        afortune.setLAST_NAME(username.substring(1, username.length()));
        afortune.setGENDER(gender);
        afortune.setBIRTH(TimeUtils.formatDate(birthday));
        int afortuneId = afortuneService.save(afortune);

        OrderSign orderSign = new OrderSign();
        orderSign.setAmount(BigDecimal.valueOf(Double.valueOf(amount)));
        orderSign.setOrder_id(WXPayUtil.generateOutTradeNo());
        orderSign.setIfortune_id(afortuneId);
        orderSign.setCreate_time(new Date());
        orderSign.setDel_flag(0);
        int orderSignId = orderSignService.save(orderSign);
        logger.info("username : {}, gender : {}, orderSignId : {}", username, gender, orderSignId);

        StringBuilder sb = new StringBuilder();
        try{
            sb.append("https://open.weixin.qq.com/connect/oauth2/authorize?");
            sb.append("appid=").append(appid);
            sb.append("&redirect_uri=").append(URLEncoder.encode(redirect_uri, "UTF-8"));
            sb.append("&response_type=code");
            sb.append("&scope=").append("snsapi_base");
            sb.append("&state=").append(String.valueOf(orderSignId)); //带自己的参数过去
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
    public Map payJSAPI(HttpServletRequest request, String code, String state, Model model) {
        logger.info("payJSAPI code = {}, state = {}", code, state);
        String total_fee = "0.01";
        String out_trade_no = "";
        if(state != null && state.length() > 0){
            OrderSign orderSign = orderSignService.findById(Integer.parseInt(state));
            total_fee = String.valueOf(orderSign.getAmount().multiply(new BigDecimal(100)).intValue());
            out_trade_no = orderSign.getOrder_id();
        }

        try {
            //页面获取openId接口
            String getopenid_url = "https://api.weixin.qq.com/sns/oauth2/access_token";
            String param = "appid=" + appid + "&secret=" + appsecret + "&code="+code+"&grant_type=authorization_code";
            //向微信服务器发送get请求获取openIdStr
            String openIdStr = HttpRequest.sendGet(getopenid_url, param);
            JSONObject json = JSONObject.parseObject(openIdStr);//转成Json格式
            String openId = json.getString("openid");//获取openId
            System.out.println("openId :" + openId);

            //拼接统一下单地址参数
            Map<String, String> paraMap = new HashMap<String, String>();
            //获取请求ip地址
            String spbill_create_ip = IPUtils.getIP(request);

            paraMap.put("appid", appid);
            paraMap.put("body", "bazi");
            paraMap.put("mch_id", mch_id);
            paraMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paraMap.put("openid", openId);
            paraMap.put("out_trade_no", out_trade_no);//订单号
            paraMap.put("spbill_create_ip", spbill_create_ip);
            paraMap.put("total_fee", total_fee);
            paraMap.put("trade_type", "JSAPI");
            paraMap.put("notify_url", notify_url);// 此路径是微信服务器调用支付结果通知路径随意写
            String sign = WXPayUtil.generateSignature(paraMap, paternerKey);
            paraMap.put("sign", sign);
            String xml = WXPayUtil.mapToXml(paraMap);//将所有参数(map)转xml格式
            System.out.println("xml : " + xml);
            // 统一下单 https://api.mch.weixin.qq.com/pay/unifiedorder
            String xmlStr = HttpRequest.sendPost(createOrderURL, xml);//发送post请求"统一下单接口"返回预支付id:prepay_id
            System.out.println("xmlStrl : " + xmlStr);
            //以下内容是返回前端页面的json数据
            String prepay_id = "";//预支付id
            if (xmlStr.indexOf("SUCCESS") != -1) {
                Map<String, String> map = WXPayUtil.xmlToMap(xmlStr);
                prepay_id = (String) map.get("prepay_id");
            }
            Map<String, String> payMap = new HashMap<String, String>();
            payMap.put("appId", appid);
            payMap.put("timeStamp", WXPayUtil.getCurrentTimestamp()+"");
            payMap.put("nonceStr", WXPayUtil.generateNonceStr());
            payMap.put("signType", "MD5");
            payMap.put("package", "prepay_id=" + prepay_id);
            String paySign = WXPayUtil.generateSignature(payMap, paternerKey);
            payMap.put("paySign", paySign);
            logger.info("prepay_id : " + prepay_id);
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
    public String payCallback (HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes){
        logger.info("payCallback");
        InputStream is = null;
        String result = "";
        try {
            is = request.getInputStream();//获取请求的流信息(这里是微信发的xml格式所有只能使用流来读)
            String xml = WXPayUtil.inputStream2String(is, "UTF-8");
            Map<String, String> notifyMap = WXPayUtil.xmlToMap(xml);//将微信发的xml转map

            if(notifyMap.get("return_code").equals("SUCCESS")){
                if(notifyMap.get("result_code").equals("SUCCESS")){
                    String outTradeNo = notifyMap.get("out_trade_no");//商户订单号
                    String transactionId = notifyMap.get("transaction_id");//微信支付订单号
                    logger.info("outTradeNo : {}, transactionId : {}", outTradeNo, transactionId);

                    // 以下是业务处理
                    OrderSign orderSign = orderSignService.findByOrderId(outTradeNo);
                    orderSign.setTransaction_id(transactionId);
                    orderSignService.update(orderSign);
                    redirectAttributes.addAttribute("ifortuneId", orderSign.getIfortune_id());
                    logger.info("order_id : {}, amount : {}, ifortuneId : {}",
                            orderSign.getOrder_id(), orderSign.getAmount(), orderSign.getIfortune_id());

                    //告诉微信服务器收到信息了，不要在调用回调action了========这里很重要回复微信服务器信息用流发送一个xml即可
                    result = "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>";
                }
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
//        return "redirect:/bazijpresult";
    }

}
