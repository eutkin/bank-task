package net.eukin.spring.lection1;

import net.eukin.spring.lection1.configuration.BeansConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class App {

    public static void main(String[] args) {
        final var ctx = new AnnotationConfigApplicationContext(BeansConfiguration.class);
    }
}
