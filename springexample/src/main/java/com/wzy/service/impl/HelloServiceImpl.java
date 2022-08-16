package com.wzy.service.impl;

import com.wzy.service.HelloService;
import org.springframework.stereotype.Service;

@Service
public class HelloServiceImpl implements HelloService {
    @Override
    public void hello() {
        System.out.println("hello spring source code!");
    }
}
