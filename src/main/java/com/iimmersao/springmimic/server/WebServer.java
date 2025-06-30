package com.iimmersao.springmimic.server;

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

}