package cn.no7player.service;

import cn.no7player.mapper.AfortuneMapper;
import cn.no7player.model.Afortune;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AfortuneService {

    @Autowired
    private AfortuneMapper afortuneMapper;

    public int save(Afortune afortune){
        afortuneMapper.insertSelective(afortune);
        return afortune.getId();
    }

    /**
     * 根据姓名和生日查询签
     * */
    public Afortune find(String name, String birth){
        Afortune afortune = afortuneMapper.find(name, birth);
        return afortune;
    }

    /**
     * 根据订单查询签
     * */
    public Afortune findByOrderId(String orderId){
        Afortune afortune = afortuneMapper.findByOrderId(orderId);
        return afortune;
    }

    /**
     * 查询
     * */
    public Afortune findById(int id){
        return afortuneMapper.selectByPrimaryKey(id);
    }

}
