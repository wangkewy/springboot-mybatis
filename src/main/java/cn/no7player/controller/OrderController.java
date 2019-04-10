package cn.no7player.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/order")
public class OrderController {

    @RequestMapping(value = "/index")
    public String index(){
        return "orderindex";
    }

}
