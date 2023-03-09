package com.wzy.controller;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;

/**
 * @author wzy
 * @date 2023年03月09日12:51:51
 */
@Controller
public class WelcomeController implements ApplicationContextAware, BeanNameAware {
    private String myName;

    private ApplicationContext applicationContext;

    @Override
    public void setBeanName(String name) {
        this.myName = name;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void handleRequest() {
        System.out.println("我的名字：" + this.myName);

        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();

        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println("容器中的BeanName：" + beanDefinitionName);
        }
    }
}
