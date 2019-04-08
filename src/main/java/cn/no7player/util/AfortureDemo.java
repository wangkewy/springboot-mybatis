package cn.no7player.util;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 艾福特恩（iFORTUNE）_命理玄学知识图谱_五路财神灵签
 * https://market.aliyun.com/products/57126001/cmapi032632.html?spm=5176.2020520132.101.2.758c7218U2Nviy#sku=yuncode2663200001
 * */
public class AfortureDemo {

    public String nameCesuan(){
        String result = "";
        String host = "http://wlcslq.market.alicloudapi.com";
        String path = "/ai_metaphysics/wu_lu_cai_shen_lin_qian/elite";
        String method = "GET";
        String appcode = "你自己的AppCode";
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
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
