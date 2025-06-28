package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.PostMapping;

import java.lang.reflect.Method;
import java.util.*;

public class Router {

    private final List<RouteEntry> routes = new ArrayList<>();

    public void registerControllers(Set<Object> controllers) {
        for (Object controller : controllers) {
            Class<?> clazz = controller.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    String path = method.getAnnotation(GetMapping.class).value();
                    RouteHandler handler = new RouteHandler(controller, method, path);
                    routes.add(new RouteEntry("GET", handler));
                } else if (method.isAnnotationPresent(PostMapping.class)) {
                    String path = method.getAnnotation(PostMapping.class).value();
                    RouteHandler handler = new RouteHandler(controller, method, path);
                    routes.add(new RouteEntry("POST", handler));
                }
            }
        }
    }

    public Optional<RouteHandler> findHandler(String method, String uri) {
        for (RouteEntry entry : routes) {
            if (entry.method().equalsIgnoreCase(method) && entry.handler().matches(uri)) {
                return Optional.of(entry.handler());
            }
        }
        return Optional.empty();
    }

    private record RouteEntry(String method, RouteHandler handler) {}
}
