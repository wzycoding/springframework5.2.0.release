package com.wzy.beanpostprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * myBeanPostProcessor Bean后置处理器
 *
 * @author wzy
 * @date 2023年01月02日11:16:42
 */
//@Configuration
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        System.out.println(beanName + "执行postProcessBeforeInitialization()方法");

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println(beanName + "执行postProcessBeforeInitialization()方法");

        return bean;
    }
}
