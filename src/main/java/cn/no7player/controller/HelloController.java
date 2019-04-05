package cn.no7player.controller;

import cn.no7player.util.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HelloController {

    @RequestMapping("/hello")
    public String greeting(@RequestParam(value="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "hello";
    }

    @RequestMapping("/bazijp")
    public String bazijp(Model model){
        return "bazijp";
    }

    @RequestMapping("/bazijpname")
    public String bazijpName(Model model){
        model.addAttribute("name", "name");

        return "bazijpname";
    }

    @RequestMapping("/bazijpresult")
    public String bazijpResult(Model model){
        model.addAttribute("name", nameCesuan());

        return "bazijpresult";
    }

    public String nameCesuan(){
        String result = "";
        String host = "http://wlcslq.market.alicloudapi.com";
        String path = "/ai_metaphysics/wu_lu_cai_shen_lin_qian/elite";
        String method = "GET";
        String appcode = "05c74640d64c4894a661c3016108e5b3";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("BIRTH", "20180808080808");
        querys.put("FIRST_NAME", "张");
        querys.put("GENDER", "男");
        querys.put("LAST_NAME", "无忌");


        try {
            /**
             * 重要提示如下:
             * HttpUtils请从
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
             * 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             */
            HttpResponse response = HttpUtils.doGet(host, path, method, headers, querys);
            System.out.println(response.toString());
            //获取response的body
            result = EntityUtils.toString(response.getEntity());
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
