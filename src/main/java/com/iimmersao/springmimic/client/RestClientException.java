package com.iimmersao.springmimic.client;

public class RestClientException extends RuntimeException {
    private final int statusCode;

    public RestClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public RestClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
