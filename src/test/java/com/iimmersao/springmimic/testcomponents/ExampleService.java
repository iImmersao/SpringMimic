package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.Component;

@Component
public class ExampleService {
    public String greet() {
        return "Hello from service!";
    }
}
