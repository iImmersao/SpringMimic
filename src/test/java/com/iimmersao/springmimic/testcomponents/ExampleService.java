package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.Service;

@Service
public class ExampleService {
    public String greet() {
        return "Hello from service!";
    }
}
