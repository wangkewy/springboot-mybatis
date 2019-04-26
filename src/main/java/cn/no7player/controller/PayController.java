package cn.no7player.controller;

import cn.no7player.util.*;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 支付接口
 * @author wk
 * @since 2019-4-8
 * */
@Controller
@RequestMapping("/pay")
public class PayController {
    private Logger logger = LoggerFactory.getLogger(PayController.class);

    @Value("${w_APPID}")
    private String APPID;

    @Value("${w_MERID}")
    private String MERID;

    @Value("${w_notify_url}")
    private String notify_url;

    @Value("${w_createOrderURL}")
    private String createOrderURL;

    @RequestMapping(value = "/go")
    public String go(String oid, String type, Model model, RedirectAttributes redirectAttributes){
        redirectAttributes.addAttribute("subject", "paySubject");
        redirectAttributes.addAttribute("totalAmount", "0.01");

        return "redirect:/pay/weixinPayWap";
    }

    @ResponseBody
    @RequestMapping(value = "/weixinPayWap" ,produces = { "application/json;charset=UTF-8" })
    public Map<String, String> weixinPayWap(HttpServletRequest request, HttpServletResponse response, ModelMap model) {
        Map<String, String> payMap = new HashMap<String, String>();
        String spbill_create_ip = getIP(request);//生产
        System.out.println("spbill_create_ip="+spbill_create_ip);
        String scene_info = "{\"h5_info\": {\"type\":\"Wap\",\"wap_url\": \"woniu8.com\",\"wap_name\": \"信息认证\"}}";//我这里是网页入口，app入口参考文档的安卓和ios写法
        String tradeType = "MWEB";//H5支付标记
        String MD5 = "MD5";//虽然官方文档不是必须参数，但是不送有时候会验签失败
        String subject = request.getParameter("subject");//前端上送的支付主题
        String total_amount = request.getParameter("totalAmount");//前端上送的支付金额
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        //金额转化为分为单位 微信支付以分为单位
        String finalmoney = StringUtils.getMoney(total_amount);
        int randomNum  = (int) (Math.random() * 1999+5000);
        String out_trade_no = TimeUtils.getSysTime("yyyyMMddHHmmss") + randomNum;
        //随机数
        String nonce_str= MD5Utils.getMessageDigest(String.valueOf(new Random().nextInt(10000)).getBytes());
        //回调地址
//        String notify_url = "xm.woniu8.com/pay/notify";
        //签名数据
        StringBuilder sb = new StringBuilder();
        sb.append("appid="+APPID);
        sb.append("&body="+subject);
        sb.append("&mch_id="+MERID);
        sb.append("&nonce_str="+nonce_str);
        sb.append("&notify_url="+notify_url);
        sb.append("&out_trade_no="+out_trade_no);
        sb.append("&scene_info="+scene_info);
        sb.append("&sign_type="+"MD5");
        sb.append("&spbill_create_ip="+spbill_create_ip);
        sb.append("&total_fee="+finalmoney);
        sb.append("&trade_type="+tradeType);
//        sb.append("&key="+SIGNKEY);
        System.out.println("sb="+sb);
        //签名MD5加密
        String sign = "把sb.toString()做MD5操作并且toUpperCase()一下,至于怎么MD5,百度一下或者看官方文档";
        System.out.println("sign="+sign);
        logger.info("签名数据:"+sign);
        //封装xml报文
        String xml="<xml>"+
                "<appid>"+ APPID+"</appid>"+
                "<mch_id>"+ MERID+"</mch_id>"+
                "<nonce_str>"+nonce_str+"</nonce_str>"+
                "<sign>"+sign+"</sign>"+
                "<body>"+subject+"</body>"+//
                "<out_trade_no>"+out_trade_no+"</out_trade_no>"+
                "<total_fee>"+finalmoney+"</total_fee>"+//
                "<trade_type>"+tradeType+"</trade_type>"+
                "<notify_url>"+notify_url+"</notify_url>"+
                "<sign_type>MD5</sign_type>"+
                "<scene_info>"+scene_info+"</scene_info>"+
                "<spbill_create_ip>"+spbill_create_ip+"</spbill_create_ip>"+
                "</xml>";

//        String createOrderURL = "https://api.mch.weixin.qq.com/pay/unifiedorder";//微信统一下单接口
        String mweb_url = "";
        Map map = new HashMap();
        try {
            //预下单 获取接口地址
            map = WebUtils.getMwebUrl(createOrderURL, xml);
            String return_code  = (String) map.get("return_code");
            String return_msg = (String) map.get("return_msg");
            if("SUCCESS".equals(return_code) && "OK".equals(return_msg)){
                mweb_url = (String) map.get("mweb_url");//调微信支付接口地址
                System.out.println("mweb_url="+mweb_url);
            }else{
                System.out.println("统一支付接口获取预支付订单出错");
                payMap.put("msg", "支付错误");
                return payMap;
            }
        } catch (Exception e) {
            System.out.println("统一支付接口获取预支付订单出错");
            payMap.put("msg", "支付错误");
            return payMap;
        }

        //支付完返回浏览器跳转的地址，如跳到查看订单页面
//        String redirect_url = "http://xm.woniu8.com/order/index";
//        try{
//            String redirect_urlEncode =  URLEncoder.encode(redirect_url,"utf-8");//对上面地址urlencode
//            mweb_url = mweb_url + "&redirect_url=" + redirect_urlEncode;//拼接返回地址
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        result.put("mwebUrl",mweb_url);

        //添加微信支付记录日志等操作


        payMap.put("msg", "success");
        payMap.put("mweb_url", mweb_url);

        return payMap;
    }

    /**
     * 获取用户实际ip,有nginx代理的情况
     * @param request
     * @return
     */
    public String getIP(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress != null) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < ipAddress.length(); i++) {
                char ch = ipAddress.charAt(i);
                if (ch != ' ')
                    buf.append(ch);
            }
            ipAddress = buf.toString();
            System.out.println("getIp x-forwarded-for");
        }

        if (ipAddress != null) {
            if (ipAddress.length() > 0 && !ipAddress.startsWith("10.")) {
                int tmpIndex = ipAddress.indexOf(",");
                if (tmpIndex > 0) {
                    ipAddress = ipAddress.substring(0, tmpIndex);
                }
                System.out.println("getIp 10.");
                return ipAddress;
            }
        }

        ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ipAddress != null) {
            int index = ipAddress.indexOf(',');
            if (index > 0) {
                ipAddress = ipAddress.substring(0, index);
            }
            System.out.println("getIp HTTP_X_FORWARDED_FOR");
            return ipAddress;
        }

        /*
         * ipAddress = request.getHeader("CLIENT_IP"); if (ipAddress == null) {
         * return request.getRemoteAddr(); } else { return ipAddress; }
         */
        ipAddress = request.getHeader("X-Real-IP");
        System.out.println("getIp X-Real-IP");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
            System.out.println("getIp X-Real-IP");
        }
        int index = ipAddress.indexOf(',');
        if (index > 0) {
            ipAddress = ipAddress.substring(0, index);
        }
        return ipAddress;
    }

    /**
     * 回调
     * */
    @RequestMapping(value = "/notify")
    public void weixinPayNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        BufferedReader reader = request.getReader();
        String line = "";
        Map map = new HashMap();
        String xml = "<xml><return_code><![CDATA[FAIL]]></xml>";
        JSONObject dataInfo = new JSONObject();
        StringBuffer inputString = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            inputString.append(line);
        }
        request.getReader().close();
        System.out.println("----接收到的报文---"+inputString.toString());
        if(inputString.toString().length()>0){
            map = XMLUtils.parseXmlToList(inputString.toString());
        }else{
            System.out.println("接受微信报文为空");
        }
        System.out.println("map="+map);
        if(map!=null && "SUCCESS".equals(map.get("result_code"))){
            //成功的业务。。。
            xml = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        }else{
            //失败的业务。。。
        }
        //告诉微信端已经确认支付成功
        response.getWriter().write(xml);
    }

}
