package com.wzy;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.wzy")
public class Main {
    public static void main(String[] args) {
//        ClassPathXmlApplicationContext applicationContext =
//                new ClassPathXmlApplicationContext("beans.xml");
//
//        UserService userService = applicationContext.getBean(UserService.class);
//
//        userService.hello("wzy");
//
//        A beanA = applicationContext.getBean(A.class);
//        B beanB = applicationContext.getBean(B.class);

        ApplicationContext context = new AnnotationConfigApplicationContext(Main.class);


    }
}
