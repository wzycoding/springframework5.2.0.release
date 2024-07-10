package com.wzy;

import com.wzy.component.PersonHandler;
import com.wzy.component.Student;
import com.wzy.component.Teacher;
import com.wzy.configuration.PersonConfiguration;
import com.wzy.selector.PersonImportBeanDefinitionRegistrar;
import com.wzy.selector.PersonImportSelector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import(PersonConfiguration.class)
@Configuration
public class AnnotationApplicationMain {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(AnnotationApplicationMain.class);

        PersonConfiguration.Person bob = (PersonConfiguration.Person) context.getBean("bobPerson");

        System.out.println(bob);
    }
}
