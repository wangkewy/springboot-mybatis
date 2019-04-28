package cn.no7player.service;

import cn.no7player.mapper.AfortuneMapper;
import cn.no7player.model.Afortune;
import cn.no7player.util.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AfortuneService {
    @Value("${i_host}")
    private String host;

    @Value("${i_path}")
    private String path;

    @Value("${i_appcode}")
    private String appcode;

    @Autowired
    private AfortuneMapper afortuneMapper;

    public int save(Afortune afortune){
        afortuneMapper.insertSelective(afortune);
        return afortune.getId();
    }

    /**
     * 根据姓名和生日查询签
     * */
    public List<Afortune> find(String name, String birth){
        return afortuneMapper.find(name, birth);
    }

    /**
     * 根据订单查询签
     * */
    public Afortune findByOrderId(String orderId){
        return afortuneMapper.findByOrderId(orderId);
    }

    /**
     * 根据订单ID查询签
     * */
    public Afortune findByOrderSignId(int orderSignId){
        return afortuneMapper.findByOrderSignId(orderSignId);
    }

    /**
     * 查询
     * */
    public Afortune findById(int id){
        return afortuneMapper.selectByPrimaryKey(id);
    }

    /**
     * 更新
     * */
    public void update(Afortune afortune){
        afortuneMapper.updateByPrimaryKeySelective(afortune);
    }

    /**
     * 艾福特恩（iFORTUNE）_命理玄学知识图谱_五路财神灵签
     * */
    public String ifortureCesuan(String birth, String firstName, String gender, String lastName){
        String result = "";
        String method = "GET";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        headers.put("x-ca-nonce", UUID.randomUUID().toString());
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("BIRTH", birth);
        querys.put("FIRST_NAME", firstName);
        querys.put("GENDER", gender);
        querys.put("LAST_NAME", lastName);

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


}
