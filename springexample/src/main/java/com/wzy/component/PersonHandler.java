package com.wzy.component;

import org.springframework.stereotype.Component;

@Component
public class PersonHandler {
    public void handle() {
        System.out.println("invoke person handler...");
        // 具体做某些处理......
    }
}
