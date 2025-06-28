package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.PostMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Router {
    private final List<RouteEntry> routes = new ArrayList<>();

    public void registerControllers(Collection<Object> controllers) {
        for (Object controller : controllers) {
            Class<?> clazz = controller.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                String httpMethod = null;
                String path = null;

                if (method.isAnnotationPresent(GetMapping.class)) {
                    httpMethod = "GET";
                    path = method.getAnnotation(GetMapping.class).value();
                } else if (method.isAnnotationPresent(PostMapping.class)) {
                    httpMethod = "POST";
                    path = method.getAnnotation(PostMapping.class).value();
                }

                if (httpMethod != null && path != null) {
                    PathPattern compiled = compilePathPattern(path);
                    RouteEntry entry = new RouteEntry(
                            compiled.regex,
                            compiled.variableNames,
                            controller,
                            method,
                            httpMethod
                    );
                    routes.add(entry);

                    System.out.printf("Registered route: [%s] %s -> %s#%s | PathVars: %s%n",
                            httpMethod, path, clazz.getSimpleName(), method.getName(), compiled.variableNames);
                }
            }
        }
    }

    public Optional<RouteHandler> findHandler(String method, String uri) {
        System.out.println("Looking for handler: " + method + " " + uri);
        for (RouteEntry entry : routes) {
            System.out.println("Testing against: " + entry.getHttpMethod() + " " + entry.getPattern());
            if (!entry.httpMethod.equalsIgnoreCase(method)) continue;
            Matcher matcher = entry.pattern.matcher(uri);
            if (matcher.matches()) {
                Map<String, String> pathVars = new HashMap<>();
                for (int i = 0; i < entry.pathVariableNames.size(); i++) {
                    pathVars.put(entry.pathVariableNames.get(i), matcher.group(i + 1));
                }
                System.out.println("Matched: " + entry.method.getName());
                return Optional.of(new RouteHandler(entry.controller, entry.method, pathVars));
            }
        }
        System.out.println("No match for: " + method + " " + uri);
        System.out.println("All registered routes:");
        for (RouteEntry entry : routes) {
            System.out.println("  [" + entry.getHttpMethod() + "] " + entry.getPattern());
        }
        return Optional.empty();
    }

    private static class PathPattern {
        final Pattern regex;
        final List<String> variableNames;

        PathPattern(Pattern regex, List<String> variableNames) {
            this.regex = regex;
            this.variableNames = variableNames;
        }
    }

    private PathPattern compilePathPattern(String pathTemplate) {
        List<String> variableNames = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{([^/}]+)}").matcher(pathTemplate);
        StringBuffer patternBuffer = new StringBuffer();

        while (matcher.find()) {
            variableNames.add(matcher.group(1));
            matcher.appendReplacement(patternBuffer, "([^/]+)");
        }

        matcher.appendTail(patternBuffer);
        String regex = "^" + patternBuffer + "$";
        System.out.printf("Compiled path: %s => regex: %s, variables: %s%n",
                pathTemplate, regex, variableNames);
        return new PathPattern(Pattern.compile(regex), variableNames);
    }
}
