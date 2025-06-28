package com.iimmersao.springmimic.server;

import com.iimmersao.springmimic.routing.Router;
import com.iimmersao.springmimic.routing.RouteHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;

import java.util.Optional;

public class WebServer extends NanoHTTPD {

    private final Router router;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebServer(int port, Router router) {
        super(port);
        this.router = router;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Optional<RouteHandler> optHandler = router.findHandler(session.getMethod().name(), session.getUri());

        if (optHandler.isPresent()) {
            try {
                RouteHandler handler = optHandler.get();
                Object result = handler.handle(session);

                if (result instanceof Response) {
                    return (Response) result;
                } else if (result instanceof String) {
                    String contentType;
                    String responseBody;

                    if (result == null) {
                        contentType = "text/plain";
                        responseBody = "";
                    } else if (result instanceof String) {
                        contentType = "text/plain";
                        responseBody = (String) result;
                    } else {
                        contentType = "application/json";
                        try {
                            responseBody = new ObjectMapper().writeValueAsString(result);
                        } catch (Exception e) {
                            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Failed to serialize JSON");
                        }
                    }

                    return newFixedLengthResponse(Response.Status.OK, contentType, responseBody);
                } else {
                    String json = objectMapper.writeValueAsString(result);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", json);
                }

            } catch (Exception e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
            }
        } else {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Route not found");
        }
    }

}