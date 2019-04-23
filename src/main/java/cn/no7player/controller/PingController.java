package cn.no7player.controller;

import cn.no7player.util.PingUtils;
import com.pingplusplus.exception.ChannelException;
import com.pingplusplus.model.Charge;
import com.pingplusplus.util.WxpubOAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//import static com.pingplusplus.Pingpp.appId;

/**
 * ping++
 * @since 2019-4-19
 * */
@RequestMapping("/ping")
@Controller
public class PingController {

    @Value("${p_appid}")
    private String appId;

    @Value("${j_notify_url}")
    private String redirectUrl;

    @Value("${j_appsecret}")
    private String appsecret;

    @RequestMapping("/wxJsapi")
    public void wxJsapi(String channel, Integer amount, Model model){
        channel = "wx_pub";
        amount = 1;
        Charge charge = createCharge(channel, amount);
        System.out.println("charge : " + charge.getDescription());
    }

    /**
     * 创建 Charge
     *
     * 创建 Charge 用户需要组装一个 map 对象作为参数传递给 Charge.create();
     * map 里面参数的具体说明请参考：https://www.pingxx.com/api#api-c-new
     * @return Charge
     */
    public Charge createCharge(String channel, int amount) {
        Charge charge = null;
//        String channel = "wx_pub";

        Map<String, Object> chargeMap = new HashMap<String, Object>();
        chargeMap.put("amount", amount);//订单总金额, 人民币单位：分（如订单总金额为 1 元，此处请填 100）
        chargeMap.put("currency", "cny");
        chargeMap.put("subject", "Your Subject");
        chargeMap.put("body", "Your Body");
        String orderNo = new Date().getTime() + PingUtils.randomString(7);
        chargeMap.put("order_no", orderNo);// 推荐使用 8-20 位，要求数字或字母，不允许其他字符
        chargeMap.put("channel", channel);// 支付使用的第三方支付渠道取值，请参考：https://www.pingxx.com/api#api-c-new
        chargeMap.put("client_ip", "127.0.0.1"); // 发起支付请求客户端的 IP 地址，格式为 IPV4，如: 127.0.0.1
        Map<String, String> app = new HashMap<String, String>();
        app.put("id", appId);
        chargeMap.put("app", app);

        // extra 取值请查看相应方法说明
        chargeMap.put("extra", channelExtra(channel));

//        try {
//            //发起交易请求
//            charge = Charge.create(chargeMap);
//            // 传到客户端请先转成字符串 .toString(), 调该方法，会自动转成正确的 JSON 字符串
//            String chargeString = charge.toString();
//            System.out.println(chargeString);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return charge;
    }

    private Map<String, Object> channelExtra(String channel) {
        Map<String, Object> extra = new HashMap<>();

        switch (channel) {
            case "wx_pub":
                extra = wxPubExtra();
                break;
        }

        return extra;
    }

    private Map<String, Object> wxPubExtra() {
        Map<String, Object> extra = new HashMap<>();
        // 可选，指定支付方式，指定不能使用信用卡支付可设置为 no_credit 。
//        extra.put("limit_pay", "no_credit");

        // 必须，用户在商户 appid 下的唯一标识。
        extra.put("open_id", getOpenid());

        return extra;
    }

    public String getOpenid() {
        String openId = "";
        System.out.println("1. 你需要有一个处理回跳的 URL");
        try {
            String url = WxpubOAuth.createOauthUrlForCode(appId, redirectUrl, false);
            System.out.println("2. 跳转到该 URL");
            System.out.println(url);

            URL wxURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) wxURL.openConnection();
            connection.connect();
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            connection.getInputStream();
            System.out.println("responseCode : " + responseCode);
            System.out.println("responseMessage : " + responseMessage);

//            openId = getOpenidWithCode("");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return openId;
    }

    public String getOpenidWithCode(String code) throws UnsupportedEncodingException {
        String openId = "";
        System.out.println("3. 微信内置浏览器会带上参数 code 跳转到你传的地址: " + redirectUrl);
        // 获取 URL 中的 code 参数
//        String code = "os823ndskelcncfyfms";
        try {
            openId = WxpubOAuth.getOpenId(appId, appsecret, code);
            System.out.println("4. 得到 openid 用于创建 charge");
        } catch (ChannelException e) {
            e.printStackTrace();
        }

        return openId;
    }

}
