package com.iimmersao.springmimic.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;
import fi.iki.elonen.NanoHTTPD;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

public class RouteHandler {
    public final Object instance;
    public final Method method;
    private final Pattern pathPattern;
    private final List<String> pathVariableNames;

    private final String httpMethod;

    public RouteHandler(Object instance, Method method, String routePath, String httpMethod) {
        this.instance = instance;
        this.method = method;
        this.httpMethod = httpMethod.toUpperCase();

        // Convert /users/{id} to /users/([^/]+)
        this.pathVariableNames = new ArrayList<>();
        String regex = Arrays.stream(routePath.split("/"))
                .filter(segment -> !segment.isEmpty())  // <<< fix: skip empty segments
                .map(segment -> {
                    if (segment.startsWith("{") && segment.endsWith("}")) {
                        String varName = segment.substring(1, segment.length() - 1);
                        pathVariableNames.add(varName);
                        return "([^/]+)";
                    } else {
                        return Pattern.quote(segment);
                    }
                })
                .reduce((a, b) -> a + "/" + b)
                .map(s -> "^/" + s + "$")
                .orElse("^/$");

        this.pathPattern = Pattern.compile(regex);

        /*
        String regex = Arrays.stream(routePath.split("/"))
                .map(segment -> {
                    if (segment.startsWith("{") && segment.endsWith("}")) {
                        String varName = segment.substring(1, segment.length() - 1);
                        pathVariableNames.add(varName);
                        return "([^/]+)";
                    } else {
                        return Pattern.quote(segment);
                    }
                })
                .reduce((a, b) -> a + "/" + b)
                .orElse("");
        this.pathPattern = Pattern.compile("^/" + regex + "$");
         */
    }

    public boolean matches(String uri) {
        return pathPattern.matcher(uri).matches();
    }

    /*
    public Object invoke(NanoHTTPD.IHTTPSession session) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        session.parseBody(new HashMap<>());
        queryParams.putAll(session.getParms());

        Matcher matcher = pathPattern.matcher(session.getUri());
        if (!matcher.matches()) {
            throw new RuntimeException("URI did not match: " + session.getUri());
        }

        Map<String, String> pathParams = new HashMap<>();
        for (int i = 0; i < pathVariableNames.size(); i++) {
            pathParams.put(pathVariableNames.get(i), matcher.group(i + 1));
        }

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            if (param.isAnnotationPresent(RequestParam.class)) {
                String key = param.getAnnotation(RequestParam.class).value();
                args[i] = convert(queryParams.get(key), param.getType());
            } else if (param.isAnnotationPresent(PathVariable.class)) {
                String key = param.getAnnotation(PathVariable.class).value();
                args[i] = convert(pathParams.get(key), param.getType());
            } else {
                args[i] = null;
            }
        }

        return method.invoke(instance, args);
    }
*/


    public Object invoke(NanoHTTPD.IHTTPSession session) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> bodyMap = new HashMap<>();
        session.parseBody(bodyMap);
        queryParams.putAll(session.getParms());

        String requestBody = bodyMap.get("postData");
        Matcher matcher = pathPattern.matcher(session.getUri());
        if (!matcher.matches()) {
            throw new RuntimeException("URI did not match: " + session.getUri());
        }

        Map<String, String> pathParams = new HashMap<>();
        for (int i = 0; i < pathVariableNames.size(); i++) {
            pathParams.put(pathVariableNames.get(i), matcher.group(i + 1));
        }

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        ObjectMapper objectMapper = new ObjectMapper();

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            if (param.isAnnotationPresent(RequestParam.class)) {
                String key = param.getAnnotation(RequestParam.class).value();
                args[i] = convert(queryParams.get(key), param.getType());

            } else if (param.isAnnotationPresent(PathVariable.class)) {
                String key = param.getAnnotation(PathVariable.class).value();
                args[i] = convert(pathParams.get(key), param.getType());

            } else if (param.isAnnotationPresent(RequestBody.class)) {
                args[i] = objectMapper.readValue(requestBody, param.getType());

            } else {
                args[i] = null;
            }
        }

        return method.invoke(instance, args);
    }



    private Object convert(String value, Class<?> type) {
        if (value == null) return null;
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        throw new RuntimeException("Unsupported param type: " + type.getName());
    }

    public boolean matches(String uri, String method) {
        return this.httpMethod.equals(method.toUpperCase()) && pathPattern.matcher(uri).matches();
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPattern() {
        return pathPattern.pattern();
    }
}
