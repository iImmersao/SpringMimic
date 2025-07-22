package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.Service;

@Service
@SuppressWarnings(value = "unused")
public class ExampleService {
    public String greet(String name) {
        return "Hello from service, " + name + "!";
    }
}
