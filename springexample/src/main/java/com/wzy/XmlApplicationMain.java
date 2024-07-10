package com.wzy;

import com.wzy.dto.Person;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class XmlApplicationMain {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring.xml");
        Person person = applicationContext.getBean(Person.class);

        System.out.println(person);
    }
}
