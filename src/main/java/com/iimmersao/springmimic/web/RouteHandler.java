package com.iimmersao.springmimic.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.RequestBody;
import com.iimmersao.springmimic.annotations.RequestParam;
import fi.iki.elonen.NanoHTTPD;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

public class RouteHandler {
    private final Object controller;
    private final Method method;
    private final Map<String, String> pathVariables;

    public RouteHandler(Object controller, Method method, Map<String, String> pathVariables) {
        this.controller = controller;
        this.method = method;
        this.pathVariables = pathVariables;
    }

    public Object handle(NanoHTTPD.IHTTPSession session) throws InvocationTargetException, IllegalAccessException, JsonProcessingException {
        var parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        Map<String, String> queryParams = session.getParms();

        for (int i = 0; i < parameters.length; i++) {
            var param = parameters[i];
            var type = param.getType();

            if (param.isAnnotationPresent(PathVariable.class)) {
                String name = param.getAnnotation(PathVariable.class).value();
                String value = pathVariables.get(name);
                args[i] = convert(value, type);

            } else if (param.isAnnotationPresent(RequestParam.class)) {
                String name = param.getAnnotation(RequestParam.class).value();
                String value = queryParams.get(name);
                args[i] = convert(value, type);

            } else if (param.isAnnotationPresent(RequestBody.class)) {
                    Class<?> bodyType = param.getType();
                    String body = readRequestBody(session); // ✅ We'll implement this
                    args[i] = new ObjectMapper().readValue(body, bodyType);

            } else {
                args[i] = null; // fallback (could later support default or request body)
            }
        }

        return method.invoke(controller, args);
    }

    private String readRequestBody(NanoHTTPD.IHTTPSession session) {
        try {
            Map<String, String> map = new HashMap<>();
            session.parseBody(map);
            return map.get("postData");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read request body", e);
        }
    }

    public Map<String, String> getPathVariables() {
        return pathVariables;
    }

    private Object convert(String value, Class<?> type) {
        if (value == null) return null;
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        // Add more types as needed
        throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
    }
}
