package cn.no7player.controller;

import cn.no7player.model.HongYin;
import cn.no7player.model.OrderSign;
import cn.no7player.service.HongYinService;
import cn.no7player.service.OrderSignService;
import cn.no7player.util.*;
import cn.no7player.util.wenxin.WXPayUtil;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
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
    private String appId;

    @Value("${w_MERID}")
    private String merId;

    @Value("${w_paternerKey}")
    public String paternerKey;

    @Value("${w_notify_url}")
    private String notifyUrl;

    @Value("${w_createOrderURL}")
    private String createOrderURL;

    @Value("${w_redirect_url}")
    private String redirectUrl;

    @Autowired
    private HongYinService hongYinService;

    @Autowired
    private OrderSignService orderSignService;

    /**
     * 生成订单数据,返回订单号
     * */
    public int makeOrderHongYin(String amount, String username, String gender, String birthday){
        HongYin hongYin = new HongYin();
        hongYin.setXing(username.substring(0, 1));
        hongYin.setMing(username.substring(1, username.length()));
        hongYin.setSex(gender);
        hongYin.setBirthday(birthday);
        int hongYinId = hongYinService.save(hongYin);
        logger.info("hongyin id : {}", hongYinId);

        OrderSign orderSign = new OrderSign();
        orderSign.setAmount(BigDecimal.valueOf(Double.valueOf(amount)));
        orderSign.setOrder_id(WXPayUtil.generateOutTradeNo());
        orderSign.setIfortune_id(hongYinId);
        orderSign.setCreate_time(new Date());
        orderSign.setDel_flag(0);
        int orderSignId = orderSignService.save(orderSign);
        logger.info("username:{}, gender : {}, orderSignId : {}", username, gender, orderSignId);

        return orderSignId;
    }

    @ResponseBody
    @RequestMapping(value = "/weixinPayWap" ,produces = { "application/json;charset=UTF-8" })
    public Map<String, String> weixinPayWap(String amount, String username, String gender, String birthday, String subject,
            HttpServletRequest request) throws Exception {
        // 生成订单数据,返回订单ID号
        int orderSignId = makeOrderHongYin(amount, username, gender, birthday);

        //生成IP
        String spbillCreateIp = getIP(request);
        logger.info("spbill_create_ip: {}", spbillCreateIp);
        //网页入口内容
        String sceneInfo = "{\"h5_info\": {\"type\":\"Wap\",\"wap_url\": \"http://www.coinus.cn\",\"wap_name\": \"2019年运势详批\"}}";
        //H5支付标记
        String tradeType = "MWEB";
        //虽然官方文档不是必须参数，但是不送有时候会验签失败
        String MD5 = "MD5";
        //支付金额转化为分为单位 微信支付以分为单位
        String finalMoney = StringUtils.getMoney(amount);
        int randomNum  = (int) (Math.random() * 1999+5000);
        String outTradeNo = TimeUtils.getSysTime("yyyyMMddHHmmss") + randomNum;
        //随机数
        String nonceStr= MD5Utils.getMessageDigest(String.valueOf(new Random().nextInt(10000)).getBytes());
        //拼接统一下单地址参数
        Map<String, String> paraMap = new HashMap<String, String>();
        paraMap.put("appid", appId);
        paraMap.put("body", subject);
        paraMap.put("mch_id", merId);
        paraMap.put("nonce_str", nonceStr);
        paraMap.put("notify_url", notifyUrl);
        paraMap.put("out_trade_no", outTradeNo);
        paraMap.put("scene_info", sceneInfo);
        paraMap.put("sign_type", MD5);
        paraMap.put("spbill_create_ip", spbillCreateIp);
        paraMap.put("total_fee", finalMoney);
        paraMap.put("trade_type", tradeType);
        String sign = WXPayUtil.generateSignature(paraMap, paternerKey);
        logger.info("MD签名后sign :"+sign);
        //封装xml报文
        String xml="<xml>"+
                "<appid>"+ appId +"</appid>"+
                "<mch_id>"+ merId +"</mch_id>"+
                "<nonce_str>"+ nonceStr +"</nonce_str>"+
                "<sign>"+ sign +"</sign>"+
                "<body>"+ subject +"</body>"+//
                "<out_trade_no>"+ outTradeNo +"</out_trade_no>"+
                "<total_fee>"+ finalMoney +"</total_fee>"+//
                "<trade_type>"+ tradeType +"</trade_type>"+
                "<notify_url>"+ notifyUrl +"</notify_url>"+
                "<sign_type>"+ MD5 +"</sign_type>"+
                "<scene_info>"+ sceneInfo +"</scene_info>"+
                "<spbill_create_ip>"+ spbillCreateIp +"</spbill_create_ip>"+
                "</xml>";
        String mwebUrl = "";
        Map<String, String> payMap = new HashMap<String, String>();
        try {
            //预下单 获取接口地址
            Map map = WebUtils.getMwebUrl(createOrderURL, xml);
            String returnCode  = (String) map.get("return_code");
            String returnMsg = (String) map.get("return_msg");
            if("SUCCESS".equals(returnCode) && "OK".equals(returnMsg)){
                //调微信支付接口地址
                mwebUrl = (String) map.get("mweb_url");
                logger.info("mweb_url: {}", mwebUrl);
                //支付完返回浏览器跳转的地址，回到结果页面 encode地址，再返回拼接地址
                mwebUrl = mwebUrl + "&redirect_url=" + URLEncoder.encode(redirectUrl + "?orderId=" + orderSignId,"utf-8");
            }else{
                logger.info("统一支付接口获取预支付订单出错");
                payMap.put("msg", "支付错误");
                return payMap;
            }
        } catch (Exception e) {
            logger.info("统一支付接口获取预支付订单出错");
            payMap.put("msg", "支付错误");
            return payMap;
        }

        payMap.put("msg", "success");
        payMap.put("mweb_url", mwebUrl);
        logger.info("last mweb_url: {}", mwebUrl);

        return payMap;
    }

    /**
     * 获取用户实际ip,有nginx代理的情况
     * @param request HttpServletRequest
     * @return String
     */
    public String getIP(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress != null) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < ipAddress.length(); i++) {
                char ch = ipAddress.charAt(i);
                if (ch != ' '){
                    buf.append(ch);
                }
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
    @ResponseBody
    public String weixinPayNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.info("weixin H5 pay notify");
        String result = "";
        try {
            //获取请求的流信息(这里是微信发的xml格式所有只能使用流来读)
            InputStream is = request.getInputStream();
            String xml = WXPayUtil.inputStream2String(is, "UTF-8");
            logger.info("weixin request xml: {}", xml);
            //将微信发的xml转map
            Map<String, String> notifyMap = WXPayUtil.xmlToMap(xml);
            if(notifyMap.get("return_code").equals("SUCCESS")){
                if(notifyMap.get("result_code").equals("SUCCESS")){
                    //商户订单号
                    String outTradeNo = notifyMap.get("out_trade_no");
                    //微信支付订单号
                    String transactionId = notifyMap.get("transaction_id");
                    logger.info("outTradeNo : {}, transactionId : {}", outTradeNo, transactionId);

                    // 以下是业务处理，更新微信订单信息
                    OrderSign orderSign = orderSignService.findByOrderId(outTradeNo);
                    orderSign.setTransaction_id(transactionId);
                    orderSignService.update(orderSign);
                    logger.info("order_id : {}, amount : {}, ifortuneId : {}",
                                orderSign.getOrder_id(), orderSign.getAmount(), orderSign.getIfortune_id());

                    //告诉微信服务器收到信息了，不要在调用回调了，这里很重要回复微信服务器信息用流发送一个xml即可
                    result = "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>";
                }
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
