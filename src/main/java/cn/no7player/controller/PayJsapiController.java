package cn.no7player.controller;

import cn.no7player.util.IPUtils;
import cn.no7player.util.wenxin.HttpRequest;
import cn.no7player.util.wenxin.WXPayUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    private Logger logger = Logger.getLogger(PayJsapiController.class);

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

    @RequestMapping("/payWX")
    public String payWX(Model model){
        StringBuilder sb = new StringBuilder();
        try{
            sb.append("https://open.weixin.qq.com/connect/oauth2/authorize?");
            sb.append("appid=").append(appid);
            sb.append("&redirect_uri=").append(URLEncoder.encode(redirect_uri, "UTF-8"));
            sb.append("&response_type=code");
            sb.append("&scope=").append("snsapi_base");
            sb.append("#wechat_redirect");
        } catch (Exception e){
            e.printStackTrace();
        }
        String url = sb.toString();
        logger.info("url : " + url);

        return "redirect:" + url;
//        return "redirect:/pay_jsapi/pay?code=one-code";
    }

    @RequestMapping("/pay")
    public String pay(HttpServletRequest request, String code, Model model){
        model.addAttribute("code", code);
        logger.info("code : " + code);

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
    public Map payJSAPI(HttpServletRequest request, String code, Model model) {
        logger.info("code : " + code);
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
//            spbill_create_ip = "127.0.0.1";

            paraMap.put("appid", appid);
            paraMap.put("body", "baziceshi");
            paraMap.put("mch_id", mch_id);
            paraMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paraMap.put("openid", openId);
            paraMap.put("out_trade_no", String.valueOf(new Date().getTime()));//订单号
            paraMap.put("spbill_create_ip", spbill_create_ip);
            paraMap.put("total_fee", "1");
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

    @RequestMapping("/payCallback")
    public void payCallback (){
        logger.info("payCallback");
    }

}
