package com.wzy.selector;

import com.wzy.component.Student;
import com.wzy.component.Teacher;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class PersonImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        GenericBeanDefinition studentBeanDefinition = new GenericBeanDefinition();
        studentBeanDefinition.setBeanClass(Student.class);
        registry.registerBeanDefinition("student", studentBeanDefinition);

        GenericBeanDefinition teacherBeanDefinition = new GenericBeanDefinition();
        teacherBeanDefinition.setBeanClass(Teacher.class);
        registry.registerBeanDefinition("teacher", teacherBeanDefinition);
    }
}
