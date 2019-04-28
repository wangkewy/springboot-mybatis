package cn.no7player.service;

import cn.no7player.mapper.OrderSignMapper;
import cn.no7player.model.OrderSign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 订单
 * */
@Service
public class OrderSignService {
    @Autowired
    private OrderSignMapper orderMapper;

    /**
     * 添加订单
     * */
    public int save(OrderSign orderSign){
        orderMapper.insertSelective(orderSign);
        return orderSign.getId();
    }

    /**
     * 更新订单
     * */
    public void update(OrderSign orderSign){
        orderMapper.updateByPrimaryKey(orderSign);
    }

    /**
     * 查询订单
     * */
    public OrderSign findById(int id){
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 根据订单号查询订单
     * */
    public OrderSign findByOrderId(String orderId){
        return orderMapper.selectByOrderId(orderId);
    }

}

