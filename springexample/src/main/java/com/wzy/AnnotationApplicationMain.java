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

@Import(PersonImportBeanDefinitionRegistrar.class)
@Configuration
public class AnnotationApplicationMain {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(AnnotationApplicationMain.class);
        String[] beanDefinitionNames = context.getBeanDefinitionNames();

        System.out.println("====输出所有bean定义信息====");
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(beanDefinitionName);
        }

        System.out.println("====获取bean对象====");
        Student student = context.getBean(Student.class);
        Teacher teacher = context.getBean(Teacher.class);

        System.out.println(student);
        System.out.println(teacher);
    }
}
