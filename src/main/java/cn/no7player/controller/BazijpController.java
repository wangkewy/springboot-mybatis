package cn.no7player.controller;

import cn.no7player.model.Afortune;
import cn.no7player.model.HongYin;
import cn.no7player.service.AfortuneService;
import cn.no7player.service.HongYinService;
import cn.no7player.util.HttpUtils;
import cn.no7player.vo.UserForm;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class BazijpController {
    private Logger logger = LoggerFactory.getLogger("BazijpController");

    @Value("${i_host}")
    private String host;

    @Value("${i_path}")
    private String path;

    @Value("${i_appcode}")
    private String appcode;

    @Autowired
    private AfortuneService afortuneService;

    @Autowired
    private HongYinService hongYinService;

    @RequestMapping("/bazijp")
    public String bazijp(Model model){
        return "bazijp";
    }

    @RequestMapping("/bazijpname")
    public String bazijpName(UserForm userForm, Model model){
        if(userForm.getUsername() != null){
            model.addAttribute("username", userForm.getUsername());
            model.addAttribute("gender", (userForm.getGender().equals("1") ? "男" : "女"));
            model.addAttribute("birthday", userForm.getBirthday());
            model.addAttribute("lDate", userForm.getlDate());
        }
        return "bazijpname";
    }

    @RequestMapping("/bazijpresult")
    public String bazijpResult(String orderSignId, Model model){
        logger.info("orderSignId : {}", orderSignId);
        if(orderSignId == null){
            return "bazijp404";
        }
        
        Afortune afortune = afortuneService.findByOrderSignId(Integer.valueOf(orderSignId));
        if(afortune == null){
            return "bazijp404";
        }

        String firstName = afortune.getFIRT_NAME();
        String lastName = afortune.getLAST_NAME();
        String birth = afortune.getBIRTH();
        String gender = afortune.getGENDER();
        String name = firstName + lastName;
        logger.info("name : {} ; birth : {}", name ,birth);

        if(StringUtils.isBlank(afortune.getSIGN_NAME())){
            //查询是否有已查过的姓名
            List<Afortune> afortuneList = afortuneService.find(name, birth);
            if(afortuneList.size() > 1){
                for(Afortune afortune1 : afortuneList){
                    if(StringUtils.isBlank(afortune1.getSIGN_NAME())){
                        afortune = copyToAfortune(afortune1, afortune);
                        afortune.setCreate_time(new Date());
                        afortuneService.update(afortune);
                        break;
                    }
                }
            } else {
                String result = afortuneService.ifortureCesuan(birth, firstName, gender, lastName);
                if(result.length() == 0 ){
                    return "bazijp404";
                }
                afortune = parseToAfortune(result, afortune);
                afortune.setCreate_time(new Date());
                afortuneService.update(afortune);
            }
        }

        if(StringUtils.isBlank(afortune.getSIGN_NAME())){
            return "bazijp404";
        }

        model.addAttribute("afortune", afortune);
        return "bazijpresult";
    }

    @RequestMapping("/bazijp_result_hongyin")
    public String bazijpOrderResult(String orderId, Model model){
        HongYin hongYin = hongYinService.findByOrderSignId(Integer.valueOf(orderId));
        hongYin = hongYinService.checkHongYinResult(hongYin);
        if(hongYin == null){
            return "bazijp404";
        }

        model.addAttribute("hongYin", hongYin);
        return "bazijpresult_hongyin";
    }

    // 解析测算的结果
    private Afortune parseToAfortune(String result, Afortune afortune){
        JSONObject jsonObject = JSON.parseObject(result);
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

    // 结果复制
    private Afortune copyToAfortune(Afortune original, Afortune target){
        target.setSIGN_NAME(original.getSIGN_NAME());
        target.setSIGN_ID(original.getSIGN_ID());
        target.setSIGN_TYPE(original.getSIGN_TYPE());
        target.setSIGN_TITLE(original.getSIGN_TITLE());
        target.setSIGN_POEM(original.getSIGN_POEM());
        target.setSIGN_INTRO(original.getSIGN_INTRO());
        target.setSIGN_ENTITY_SIGN_CAREER(original.getSIGN_ENTITY_SIGN_CAREER());
        target.setSIGN_ENTITY_SIGN_FAMILY(original.getSIGN_ENTITY_SIGN_FAMILY());
        target.setSIGN_ENTITY_SIGN_EMOTION(original.getSIGN_ENTITY_SIGN_EMOTION());
        target.setSIGN_ENTITY_SIGN_ACADEMIC(original.getSIGN_ENTITY_SIGN_ACADEMIC());
        target.setSIGN_ENTITY_SIGN_INVEST(original.getSIGN_ENTITY_SIGN_INVEST());
        target.setSIGN_ENTITY_SIGN_HEALTH(original.getSIGN_ENTITY_SIGN_HEALTH());
        target.setSIGN_ENTITY_SIGN_SWITCH(original.getSIGN_ENTITY_SIGN_SWITCH());
        target.setSIGN_ENTITY_SIGN_LAWSUIT(original.getSIGN_ENTITY_SIGN_LAWSUIT());
        target.setSIGN_ENTITY_SIGN_LOST(original.getSIGN_ENTITY_SIGN_LOST());
        target.setSIGN_ENTITY_SIGN_TRAVEL(original.getSIGN_ENTITY_SIGN_TRAVEL());
        target.setSIGN_ENTITY_SIGN_CHILD(original.getSIGN_ENTITY_SIGN_CHILD());

        return target;
    }

}
