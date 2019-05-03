package cn.no7player.util.demo;

import cn.no7player.util.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 洪铟八字算命官网DEMO
 * https://market.aliyun.com/products/57126001/cmapi027257.html?spm=5176.730006-56956004-57126001-cmapi027257.content.11.66317facvTbNjx&innerSource=search_�%7D&accounttraceid=4457378d-057c-4721-9c88-d1e43f03b744#sku=yuncode2125700007
 * */
public class HongYin {

    public static void main(String[] args) {
        String host = "https://openapi.fatebox.cn";
        String path = "/openapi/Name/getName";
        String method = "GET";
        String appcode = "你自己的AppCode";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("birthday", "1989-11-11");
        querys.put("hour", "19");
        querys.put("ming", "明明");
        querys.put("minute", "34");
        querys.put("sex", "女");
        querys.put("xing", "陈");


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
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
