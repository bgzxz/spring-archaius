package io.docbot;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SpringTest {
    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-context.xml");
        ValTest test = applicationContext.getBean(ValTest.class);
        BufferedReader b = new BufferedReader(new InputStreamReader(System.in));
        String path = b.readLine();
        while (path != null || !path.isEmpty()) {
            test.show();
            path = b.readLine();
        }
        test.show();
    }
}
