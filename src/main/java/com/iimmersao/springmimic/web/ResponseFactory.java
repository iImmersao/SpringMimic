package com.iimmersao.springmimic.web;

import fi.iki.elonen.NanoHTTPD;

public class ResponseFactory {

    public static NanoHTTPD.Response ok(String body) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", body);
    }

    public static NanoHTTPD.Response jsonOk(String json) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", json);
    }

    public static NanoHTTPD.Response badRequest(String message) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", message);
    }

    public static NanoHTTPD.Response unauthorized(String message) {
        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.UNAUTHORIZED, "text/plain", message);
        response.addHeader("WWW-Authenticate", "Basic realm=\"Access to the API\"");
        return response;
    }

    public static NanoHTTPD.Response forbidden(String message) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.FORBIDDEN, "text/plain", message);
    }

    public static NanoHTTPD.Response notFound(String message) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", message);
    }

    public static NanoHTTPD.Response internalServerError(String message) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", message);
    }
}
