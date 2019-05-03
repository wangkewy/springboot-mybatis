package cn.no7player.service;

import cn.no7player.mapper.HongYinMapper;
import cn.no7player.model.HongYin;
import cn.no7player.util.HttpUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HongYinService {
    @Value("${h_host}")
    private String host;

    @Value("${h_path}")
    private String path;

    @Value("${h_appcode}")
    private String appcode;

    @Autowired
    private HongYinMapper hongYinMapper;

    /**
     * 保存
     * */
    public int save(HongYin hongYin){
        hongYinMapper.insertSelective(hongYin);
        return hongYin.getId();
    }

    /**
     * 检查姓名测试结果
     * */
    public HongYin checkHongYinResult(HongYin hongYin){
        String xing = hongYin.getXing();
        String ming = hongYin.getMing();
        String sex = hongYin.getSex();
        String birthday = hongYin.getBirthday();

        if(StringUtils.isBlank(hongYin.getZongge())){
            //查询是否有已查过的姓名
            List<HongYin> hongYinList = findSelective(xing, ming, sex, birthday);
            if(hongYinList.size() > 1){
                for (HongYin _hongYin : hongYinList){
                    if(StringUtils.isNotBlank(_hongYin.getZongge())){
                        hongYin = copyToHongYin(_hongYin, hongYin);
                        hongYin.setCreate_time(new Date());
                        hongYinMapper.updateByPrimaryKey(hongYin);
                        break;
                    }
                }
            } else {
                String result = cesuan(birthday, hongYin.getHour(), ming, hongYin.getMinute(), sex, xing);
                hongYin = parseResult(result, hongYin);
                hongYin.setCreate_time(new Date());
                hongYinMapper.updateByPrimaryKey(hongYin);
            }
        }

        return hongYin;
    }

    public HongYin findByOrderSignId(int orderSignId){
        return hongYinMapper.findByOrderSignId(orderSignId);
    }

    /**
     * 查询
     * @param xing 姓
     * @param ming 名
     * @param birthday 生日
     * */
    public List<HongYin> findSelective(String xing, String ming, String sex, String birthday){
        return hongYinMapper.findSelective(xing, ming, sex, birthday);
    }

    /**
     * 洪铟姓名测试
     * */
    public String cesuan(String birthday, String hour, String ming, String minute, String sex, String xing){
        String result = "";
//        String host = "https://openapi.fatebox.cn";
//        String path = "/openapi/Name/getName";
        String method = "GET";
//        String appcode = "你自己的AppCode";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("birthday", birthday);
        //"1989-11-11"
        querys.put("hour", hour);
        //"19"
        querys.put("ming", ming);
        // "明明"
        querys.put("minute", minute);
        /** "34"*/
        querys.put("sex", sex);
        // "女"
        querys.put("xing", xing);
        // "陈"
        try {
            HttpResponse response = HttpUtils.doGet(host, path, method, headers, querys);
            System.out.println(response.toString());
            //获取response的body
            result = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 属性复制
     * */
    private HongYin copyToHongYin(HongYin original, HongYin target){
        target.setXings(original.getXings());
        target.setMings(original.getMings());
        target.setShengyun(original.getShengyun());
        target.setTiange(original.getTiange());
        target.setRenge(original.getRenge());
        target.setDige(original.getDige());
        target.setWaige(original.getWaige());
        target.setZongge(original.getZongge());
        target.setZonglun(original.getZonglun());
        target.setPingfen(original.getPingfen());
        target.setJichuyun(original.getJichuyun());
        target.setChenggongyun(original.getChenggongyun());
        target.setShejiaoyun(original.getShejiaoyun());

        return target;
    }

    /**
     * 解析测算的结果
     * */
    private HongYin parseResult(String result, HongYin hongYin){
        JSONObject jsonObject = JSON.parseObject(result);
        hongYin.setXings(jsonObject.getString("Xings"));
        hongYin.setMings(jsonObject.getString("Mings"));
        hongYin.setShengyun(jsonObject.getString("ShengYun"));
        hongYin.setTiange(jsonObject.getString("TianGe"));
        hongYin.setRenge(jsonObject.getString("RenGe"));
        hongYin.setDige(jsonObject.getString("DiGe"));
        hongYin.setWaige(jsonObject.getString("WaiGe"));
        hongYin.setZongge(jsonObject.getString("ZongGe"));
        hongYin.setZonglun(jsonObject.getString("ZongLun"));
        hongYin.setPingfen(jsonObject.getString("PingFen"));
        hongYin.setJichuyun(jsonObject.getString("JiChuYun"));
        hongYin.setChenggongyun(jsonObject.getString("ChengGongYun"));
        hongYin.setShejiaoyun(jsonObject.getString("SheJiaoYun"));

        return hongYin;
    }

}
