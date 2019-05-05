package cn.no7player.service;

import cn.no7player.mapper.OrderSignMapper;
import cn.no7player.model.HongYin;
import cn.no7player.model.OrderSign;
import cn.no7player.util.wenxin.WXPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单
 * */
@Service
public class OrderSignService {
    private Logger logger = LoggerFactory.getLogger(OrderSignService.class);

    @Autowired
    private OrderSignMapper orderMapper;

    @Autowired
    private HongYinService hongYinService;

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

    /**
     * 生成洪铟和订单数据,返回订单
     * */
    public OrderSign makeOrderHongYin(String amount, String username, String gender, String birthday, String payType){
        HongYin hongYin = new HongYin();
        hongYin.setXing(username.substring(0, 1));
        hongYin.setMing(username.substring(1, username.length()));
        hongYin.setSex(gender);
        hongYin.setBirthday(birthday);
        int hongYinId = hongYinService.save(hongYin);
        logger.info("hongyin id : {}", hongYinId);

        OrderSign orderSign = new OrderSign();
        orderSign.setAmount(BigDecimal.valueOf(Double.valueOf(amount)));
        orderSign.setOrder_id(WXPayUtil.generateOutTradeNo());
        orderSign.setIfortune_id(hongYinId);
        orderSign.setCreate_time(new Date());
        orderSign.setDel_flag(0);
        orderSign.setPay_type(payType);
        int orderSignId = save(orderSign);
        logger.info("username:{}, gender : {}, orderSignId : {}", username, gender, orderSignId);

        return orderSign;
    }

}

