package com.iimmersao.springmimic.routing;

import java.util.regex.Matcher;

public class RouteMatch {
    private final RouteHandler handler;
    private final Matcher matcher;

    public RouteMatch(RouteHandler handler, Matcher matcher) {
        this.handler = handler;
        this.matcher = matcher;
    }

    public RouteHandler getHandler() {
        return handler;
    }

    public Matcher getMatcher() {
        return matcher;
    }
}