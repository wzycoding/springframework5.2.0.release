package com.wzy.circle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//@Service
public class B {

    @Autowired
    private A a;

    public B() {
        System.out.println("create B instance");
    }

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }
}
