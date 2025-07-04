package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Router {
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+)}");

    private final List<RouteEntry> routes = new ArrayList<>();

    private Pattern pathToRegex(String path) {
        String regex = PATH_VARIABLE_PATTERN.matcher(path).replaceAll("([^/]+)");
        return Pattern.compile("^" + regex + "$");
    }

    public void registerControllers(Collection<Object> controllers) {
        for (Object controller : controllers) {
            Class<?> clazz = controller.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                Annotation[] annotations = method.getAnnotations();
                for (Annotation annotation : annotations) {
                    String httpMethod = null;
                    String path = null;

                    if (annotation.annotationType().equals(GetMapping.class)) {
                        httpMethod = "GET";
                        path = ((GetMapping) annotation).value();
                    } else if (annotation.annotationType().equals(PostMapping.class)) {
                        httpMethod = "POST";
                        path = ((PostMapping) annotation).value();
                    } else if (annotation.annotationType().equals(PutMapping.class)) {
                        httpMethod = "PUT";
                        path = ((PutMapping) annotation).value();
                    } else if (annotation.annotationType().equals(PatchMapping.class)) {
                        httpMethod = "PATCH";
                        path = ((PatchMapping) annotation).value();
                    } else if (annotation.annotationType().equals(DeleteMapping.class)) {
                        httpMethod = "DELETE";
                        path = ((DeleteMapping) annotation).value();
                    }

                    if (httpMethod != null && path != null) {
                        Pattern regexPattern = pathToRegex(path);
                        RouteHandler handler = new RouteHandler(httpMethod, path, controller, method);
                        routes.add(new RouteEntry(httpMethod, regexPattern, handler));
                    }
                }
            }
        }
    }

    public RouteMatch findHandler(String method, String uri) {
        for (RouteEntry entry : routes) {
            if (entry.httpMethod().equalsIgnoreCase(method)) {
                Matcher matcher = entry.pattern().matcher(uri);
                if (matcher.matches()) {
                    return new RouteMatch(entry.handler(), matcher);
                }
            }
        }
        return null;
    }

    private record RouteEntry(String httpMethod, Pattern pattern, RouteHandler handler) {}
}
