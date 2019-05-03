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
    private String APPID;

    @Value("${w_MERID}")
    private String MERID;

    @Value("${w_notify_url}")
    private String notify_url;

    @Value("${w_createOrderURL}")
    private String createOrderURL;

    @Value("${w_redirect_url}")
    private String redirectUrl;

    @Autowired
    private HongYinService hongYinService;

    @Autowired
    private OrderSignService orderSignService;

    @RequestMapping(value = "/go")
    public String go(String oid, String type, Model model, RedirectAttributes redirectAttributes){
        redirectAttributes.addAttribute("subject", "paySubject");
        redirectAttributes.addAttribute("totalAmount", "0.01");

        return "redirect:/pay/weixinPayWap";
    }

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
    public Map<String, String> weixinPayWap(String amount, String username, String gender, String birthday,
            HttpServletRequest request, HttpServletResponse response, ModelMap model) {
        // 生成订单数据,返回订单ID号
        int orderSignId = makeOrderHongYin(amount, username, gender, birthday);

        Map<String, String> payMap = new HashMap<String, String>();
        //生成IP
        String spbill_create_ip = getIP(request);
        System.out.println("spbill_create_ip="+spbill_create_ip);
        //我这里是网页入口，app入口参考文档的安卓和ios写法
        String scene_info = "{\"h5_info\": {\"type\":\"Wap\",\"wap_url\": \"woniu8.com\",\"wap_name\": \"信息认证\"}}";
        //H5支付标记
        String tradeType = "MWEB";
        //虽然官方文档不是必须参数，但是不送有时候会验签失败
        String MD5 = "MD5";
        //前端上送的支付主题
        String subject = request.getParameter("八字运势");
        //前端上送的支付金额 金额转化为分为单位 微信支付以分为单位
        String finalmoney = StringUtils.getMoney(amount);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        int randomNum  = (int) (Math.random() * 1999+5000);
        String outTradeNo = TimeUtils.getSysTime("yyyyMMddHHmmss") + randomNum;
        //随机数
        String nonceStr= MD5Utils.getMessageDigest(String.valueOf(new Random().nextInt(10000)).getBytes());
        //签名数据
        StringBuilder sb = new StringBuilder();
        sb.append("appid="+APPID);
        sb.append("&body="+subject);
        sb.append("&mch_id="+MERID);
        sb.append("&nonce_str="+nonceStr);
        sb.append("&notify_url="+notify_url);
        sb.append("&out_trade_no="+outTradeNo);
        sb.append("&scene_info="+scene_info);
        sb.append("&sign_type="+"MD5");
        sb.append("&spbill_create_ip="+spbill_create_ip);
        sb.append("&total_fee="+finalmoney);
        sb.append("&trade_type="+tradeType);
//        sb.append("&key="+SIGNKEY);
        System.out.println("sb="+sb);
        //签名MD5加密
        String sign = "把sb.toString() 做MD5操作并且toUpperCase()一下,至于怎么MD5,百度一下或者看官方文档";
        System.out.println("sign="+sign);
        logger.info("签名数据:"+sign);
        //封装xml报文
        String xml="<xml>"+
                "<appid>"+ APPID+"</appid>"+
                "<mch_id>"+ MERID+"</mch_id>"+
                "<nonce_str>"+nonceStr+"</nonce_str>"+
                "<sign>"+sign+"</sign>"+
                "<body>"+subject+"</body>"+//
                "<out_trade_no>"+outTradeNo+"</out_trade_no>"+
                "<total_fee>"+finalmoney+"</total_fee>"+//
                "<trade_type>"+tradeType+"</trade_type>"+
                "<notify_url>"+notify_url+"</notify_url>"+
                "<sign_type>MD5</sign_type>"+
                "<scene_info>"+scene_info+"</scene_info>"+
                "<spbill_create_ip>"+spbill_create_ip+"</spbill_create_ip>"+
                "</xml>";

        String mwebUrl = "";
        try {
            //预下单 获取接口地址
            Map map = new HashMap();
            map = WebUtils.getMwebUrl(createOrderURL, xml);
            String returnCode  = (String) map.get("return_code");
            String returnMsg = (String) map.get("return_msg");
            if("SUCCESS".equals(returnCode) && "OK".equals(returnMsg)){
                //调微信支付接口地址
                mwebUrl = (String) map.get("mweb_url");
                System.out.println("mweb_url="+mwebUrl);
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

        //支付完返回浏览器跳转的地址，回到结果页面
        try{
            //encode地址，再返回拼接地址
            mwebUrl = mwebUrl + "&redirect_url=" + URLEncoder.encode(redirectUrl + "&orderId=" + orderSignId,"utf-8");
        }catch (Exception e){
            e.printStackTrace();
        }

        payMap.put("msg", "success");
        payMap.put("mweb_url", mwebUrl);
        logger.info("mweb_url : {}", mwebUrl);

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
    public void weixinPayNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.info("weixin pay notify");
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
