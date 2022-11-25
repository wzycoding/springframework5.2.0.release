package com.wzy.dispose;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomDisposableBean implements DisposableBean {
    @Override
    public void destroy() {
        System.out.println("CustomDisposableBean===>destroy()");
    }
}
