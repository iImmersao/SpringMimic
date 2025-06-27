package com.iimmersao.springmimic.web;

import com.iimmersao.springmimic.annotations.Controller;
import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.PostMapping;

import java.lang.reflect.Method;
import java.util.*;

public class Router {

    private final List<RouteHandler> handlers = new ArrayList<>();

    // web/Router.java
    public void registerControllers(Collection<Object> beans) {
        for (Object bean : beans) {
            Class<?> clazz = bean.getClass();
            if (clazz.isAnnotationPresent(Controller.class)) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        String path = method.getAnnotation(GetMapping.class).value();
                        System.out.println("Registering route [GET] " + path + " -> " + method);
                        handlers.add(new RouteHandler(bean, method, path, "GET"));
                    } else if (method.isAnnotationPresent(PostMapping.class)) {
                        String path = method.getAnnotation(PostMapping.class).value();
                        System.out.println("Registering route [POST] " + path + " -> " + method);
                        handlers.add(new RouteHandler(bean, method, path, "POST"));
                    }
                }
            }
        }
    }

    /*
    public Optional<RouteHandler> findHandler(String uri, String method) {
        return handlers.stream().filter(h -> h.matches(uri, method)).findFirst();
    }
    */

    public Optional<RouteHandler> findHandler(String uri, String method) {
        System.out.println("Looking for handler: " + method + " " + uri);
        for (RouteHandler handler : handlers) {
            System.out.println("Testing against: " + handler.getHttpMethod() + " " + handler.getPattern());
            if (handler.matches(uri, method)) {
                System.out.println("Matched: " + handler.method.getName());
                return Optional.of(handler);
            }
        }
        System.out.println("No match for: " + method + " " + uri);
        System.out.println("All registered routes:");
        for (RouteHandler handler : handlers) {
            System.out.println("  [" + handler.getHttpMethod() + "] " + handler.getPattern());
        }
        return Optional.empty();
    }
}
