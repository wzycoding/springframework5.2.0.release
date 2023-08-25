package com.wzy.controller;

import com.wzy.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @author wzy
 */
@Controller
public class HelloController {

    @Autowired
    private HelloService helloService;


    public void handleHello() {
        helloService.hello();;
    }
}
