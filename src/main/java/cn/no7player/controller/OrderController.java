package cn.no7player.controller;

import cn.no7player.service.AfortuneService;
import cn.no7player.service.OrderSignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/order")
public class OrderController {
    @Autowired
    private OrderSignService orderService;

    @Autowired
    private AfortuneService afortuneService;

    @RequestMapping(value = "/index")
    public String index(){
        return "orderindex";
    }


}
