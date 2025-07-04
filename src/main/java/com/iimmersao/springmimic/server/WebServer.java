package com.iimmersao.springmimic.server;

import com.iimmersao.springmimic.core.ExceptionHandler;
import com.iimmersao.springmimic.exceptions.RouteNotFoundException;
import com.iimmersao.springmimic.routing.Router;
import com.iimmersao.springmimic.routing.RouteMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

    private final Router router;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebServer(int port, Router router) {
        super(port);
        this.router = router;
    }

    /*
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().name();

        RouteMatch match = router.findHandler(method, uri);
        if (match != null) {
            return match.getHandler().handle(session, match.getMatcher());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Route not found");
    }
     */

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().name();

        try {
            RouteMatch match = router.findHandler(method, uri);
            if (match != null) {
                return match.getHandler().handle(session, match.getMatcher());
            }

            throw new RouteNotFoundException("No route matched for " + method + " " + uri);
        } catch (Exception e) {
            return ExceptionHandler.handle(e);
        }
    }
}