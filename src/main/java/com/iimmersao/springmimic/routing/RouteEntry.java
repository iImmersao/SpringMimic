package com.iimmersao.springmimic.routing;

//package core.routing;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

public class RouteEntry {
    public Pattern pattern;
    public List<String> pathVariableNames;
    public Object controller;
    public Method method;
    public String httpMethod;

    public RouteEntry(Pattern pattern, List<String> pathVariableNames, Object controller,
                      Method method, String httpMethod) {
        this.pattern = pattern;
        this.pathVariableNames = pathVariableNames;
        this.controller = controller;
        this.method = method;
        this.httpMethod = httpMethod;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public List<String> getPathVariableNames() {
        return pathVariableNames;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public String getHttpMethod() {
        return httpMethod;
    }
}
