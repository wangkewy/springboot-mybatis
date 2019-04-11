package cn.no7player.service;

import cn.no7player.mapper.OrderSignMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 订单
 * */
@Service
public class OrderSignService {
    @Autowired
    private OrderSignMapper orderMapper;


}

