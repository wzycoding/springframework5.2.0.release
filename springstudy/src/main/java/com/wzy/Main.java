package com.wzy;

import com.wzy.service.UserService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.FileSystemXmlApplicationContext;

@Configuration
@ComponentScan("com.wzy")
public class Main {
//    public static void main(String[] args) {
////        ClassPathXmlApplicationContext applicationContext =
////                new ClassPathXmlApplicationContext("beans.xml");
////
////        UserService userService = applicationContext.getBean(UserService.class);
////
////        userService.hello("wzy");
////
////        A beanA = applicationContext.getBean(A.class);
////        B beanB = applicationContext.getBean(B.class);
//
//        ApplicationContext context = new AnnotationConfigApplicationContext(Main.class);
//
//
//    }

    public static void main(String[] args) {
        String configFilePath = "//Users/wzy/project/source-studty/springframework5.2.0.release/springstudy/src/main/resources/beans.xml";
        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(configFilePath);

        String[] beanDefinitionNames = context.getBeanDefinitionNames();

        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }

        UserService userService1a = (UserService) context.getBean("userService1");
        UserService userService1b = (UserService) context.getBean("userService1");

        UserService userService2a = (UserService) context.getBean("userService2");
        UserService userService2b = (UserService) context.getBean("userService2");

        UserService userService3a = (UserService) context.getBean("userService3");
        UserService userService3b = (UserService) context.getBean("userService3");

        UserService userService4a = (UserService) context.getBean("userService4");
        UserService userService4b = (UserService) context.getBean("userService4");


        System.out.println("无参构造函数创建的对象：" + userService1a);
        System.out.println("无参构造函数创建的对象：" + userService1b);

        System.out.println("静态工厂创建的对象：" + userService2a);
        System.out.println("静态工厂创建的对象：" + userService2b);

        System.out.println("实例工厂创建的对象：" + userService3a);
        System.out.println("实例工厂创建的对象：" + userService3b);

        System.out.println("FactoryBean创建的对象：" + userService4a);
        System.out.println("FactoryBean创建的对象：" + userService4b);
    }


//    public static void main(String[] args) {
//        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Main.class);
//
//        String[] beanDefinitionNames = context.getBeanDefinitionNames();
//
//        for (String beanDefinitionName : beanDefinitionNames) {
//            System.out.println(beanDefinitionName);
//        }
//    }
}
