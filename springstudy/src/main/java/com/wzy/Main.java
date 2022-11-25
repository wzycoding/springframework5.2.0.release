package com.wzy;

import com.wzy.service.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext =
                new ClassPathXmlApplicationContext("beans.xml");

        UserService userService = applicationContext.getBean(UserService.class);

        userService.hello("wzy");
    }
}
