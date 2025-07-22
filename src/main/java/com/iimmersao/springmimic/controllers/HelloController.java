package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.*;

@Controller
@SuppressWarnings(value = "unused")
public class HelloController {

    @GetMapping("/greet")
    public String greet(@RequestParam("name") String name) {
        return "Hello, " + name;
    }

    @GetMapping("/add")
    public String add(@RequestParam("a") int a, @RequestParam("b") int b) {
        return "Sum: " + (a + b);
    }
}
