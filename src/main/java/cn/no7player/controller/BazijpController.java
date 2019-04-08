package cn.no7player.controller;

import cn.no7player.model.Afortune;
import cn.no7player.service.AfortuneService;
import cn.no7player.util.HttpUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class BazijpController {
    private Logger logger = Logger.getLogger(BazijpController.class);

    @Autowired
    private AfortuneService afortuneService;

    @RequestMapping("/bazijp")
    public String bazijp(Model model){
        return "bazijp";
    }

    @RequestMapping("/bazijpname")
    public String bazijpName(String username, String gender, String birthday, Model model){
        model.addAttribute("username", username);
        model.addAttribute("gender", gender);
        model.addAttribute("birthday", birthday);

        return "bazijpname";
    }

    @RequestMapping("/bazijpresult")
    public String bazijpResult(Model model){
        String name = "张无忌";
        String birth = "20180808";
        logger.info("name : " + name + " ; birth : " + birth);
        Afortune afortune = afortuneService.find(name, birth);
        if(afortune == null){
            String result = ifortureCesuan("","","","");
            if(result.length() > 0){
                afortune = parseToAfortune(result);
                afortuneService.save(afortune);
            }
        }

        if(afortune != null){
            model.addAttribute("afortune", afortune);
            return "bazijpresult";
        } else {
            return "bazijp404";
        }
    }

    /**
     * 艾福特恩（iFORTUNE）_命理玄学知识图谱_五路财神灵签
     * */
    private String ifortureCesuan(String birth, String firstName, String gender, String lastName){
        String result = "";
        String host = "http://wlcslq.market.alicloudapi.com";
        String path = "/ai_metaphysics/wu_lu_cai_shen_lin_qian/elite";
        String method = "GET";
        String appcode = "05c74640d64c4894a661c3016108e5b3";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        headers.put("x-ca-nonce", UUID.randomUUID().toString());
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("BIRTH", "20180808080808");
        querys.put("FIRST_NAME", "张");
        querys.put("GENDER", "男");
        querys.put("LAST_NAME", "无忌");

        try {
            HttpResponse response = HttpUtils.doGet(host, path, method, headers, querys);
            //获取response的body
            int statusline = response.getStatusLine().getStatusCode();
            if(statusline == 200){
                result = EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // 解析测算的结果
    private Afortune parseToAfortune(String result){
        JSONObject jsonObject = JSON.parseObject(result);
        Afortune afortune = new Afortune();
        afortune.setFIRT_NAME(jsonObject.getString("FIRT_NAME"));
        afortune.setLAST_NAME(jsonObject.getString("LAST_NAME"));
        afortune.setName(afortune.getFIRT_NAME() + afortune.getLAST_NAME());
        afortune.setBIRTH(jsonObject.getString("BIRTH"));
        afortune.setNONGLI_YEAR(jsonObject.getString("YEAR"));
        afortune.setNONGLI_MONTH(jsonObject.getString("MONTH"));
        afortune.setNONGLI_DAY(jsonObject.getString("DAY"));
        afortune.setNONGLI_HOUR(jsonObject.getString("HOUR"));
        afortune.setANIMAL(jsonObject.getString("ANIMAL"));
        afortune.setGENDER(jsonObject.getString("GENDER"));
        afortune.setSIGN_NAME(jsonObject.getString("SIGN_NAME"));
        afortune.setSIGN_ID(jsonObject.getString("SIGN_ID"));
        afortune.setSIGN_TYPE(jsonObject.getString("SIGN_TYPE"));
        afortune.setSIGN_TITLE(jsonObject.getString("SIGN_TITLE"));
        afortune.setSIGN_POEM(jsonObject.getString("SIGN_POEM"));
        afortune.setSIGN_INTRO(jsonObject.getString("SIGN_INTRO"));
        afortune.setSIGN_ENTITY_SIGN_CAREER(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_CAREER"));
        afortune.setSIGN_ENTITY_SIGN_FAMILY(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_FAMILY"));
        afortune.setSIGN_ENTITY_SIGN_EMOTION(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_EMOTION"));
        afortune.setSIGN_ENTITY_SIGN_ACADEMIC(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_ACADEMIC"));
        afortune.setSIGN_ENTITY_SIGN_INVEST(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_INVEST"));
        afortune.setSIGN_ENTITY_SIGN_HEALTH(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_HEALTH"));
        afortune.setSIGN_ENTITY_SIGN_SWITCH(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_SWITCH"));
        afortune.setSIGN_ENTITY_SIGN_LAWSUIT(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_LAWSUIT"));
        afortune.setSIGN_ENTITY_SIGN_LOST(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_LOST"));
        afortune.setSIGN_ENTITY_SIGN_TRAVEL(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_TRAVEL"));
        afortune.setSIGN_ENTITY_SIGN_CHILD(jsonObject.getJSONObject("SIGN_ENTITY").getString("SIGN_CHILD"));

        return afortune;
    }

}
