package com.wzy.circle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class A {

    @Autowired
    private B b;

    public A() {
        System.out.println("create A instance");
    }

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }

}
