package com.iimmersao.springmimic.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iimmersao.springmimic.exceptions.RouteNotFoundException;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fi.iki.elonen.NanoHTTPD.Response.Status;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.Map;

public class ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    public static NanoHTTPD.Response handle(Exception e) {

        if (e instanceof IllegalArgumentException) {
            log.warn("Client error: {}", e.getMessage());
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    "application/json",
                    "{\"error\":\"" + e.getMessage() + "\"}"
            );
        }

        if (e instanceof JsonProcessingException) {
            String rootMessage = "Invalid request body";
            String details = e.getMessage(); // Jackson-specific message
            log.warn(rootMessage + ": " + e.getMessage());
            return createErrorResponse(NanoHTTPD.Response.Status.BAD_REQUEST, rootMessage, details);
        }

        if (e instanceof UnsupportedOperationException) {
            log.warn("Unsupported operation: {}", e.getMessage());
            return NanoHTTPD.newFixedLengthResponse(Status.NOT_IMPLEMENTED, "text/plain", e.getMessage());
        }

        if (e instanceof RouteNotFoundException) {
            String rootMessage = "Route not found";
            log.warn("Route not found: {}", e.getMessage());
            return createErrorResponse(NanoHTTPD.Response.Status.NOT_FOUND, rootMessage, e.getMessage());
        }
        // Generic fallback
        log.error("Internal server error: ", e); // Full stack trace here
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                "application/json",
                "{\"error\":\"Internal server error\"}"
        );
    }

    private static NanoHTTPD.Response createErrorResponse(NanoHTTPD.Response.Status status, String message, String details) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", message);
        if (details != null && !details.isBlank()) {
            errorBody.put("details", details);
        }

        String json;
        try {
            json = new ObjectMapper().writeValueAsString(errorBody);
        } catch (Exception ex) {
            json = "{\"error\":\"" + message + "\"}";
        }

        return NanoHTTPD.newFixedLengthResponse(status, "application/json", json);
    }
}
