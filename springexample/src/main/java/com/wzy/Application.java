package com.wzy;


import com.wzy.controller.HelloController;
import com.wzy.service.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.wzy")
public class Application {

	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(Application.class);

		HelloController helloController = context.getBean(HelloController.class);
		helloController.handleHello();
	}
}
