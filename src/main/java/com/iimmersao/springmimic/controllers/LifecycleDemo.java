package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.*;

@Controller
public class LifecycleDemo {

    @PostConstruct
    public void init() {
        System.out.println("LifecycleDemo initialized.");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("LifecycleDemo shutting down.");
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
