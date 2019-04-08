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

    public void save(Afortune afortune){
        afortuneMapper.insert(afortune);
    }

    public Afortune find(String name, String birth){
        Afortune afortune = afortuneMapper.find(name, birth);
        return afortune;
    }
}
