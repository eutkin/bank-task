package net.eukin.spring.lection1.service;

import org.springframework.util.StringUtils;

public class CapitalizePrettyPrinter implements PrettyPrinter {

    @Override
    public String doNameAsPretty(String name) {
        return StringUtils.capitalize(name);
    }
}
