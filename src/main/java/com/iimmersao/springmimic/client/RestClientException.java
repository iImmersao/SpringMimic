package com.iimmersao.springmimic.client;

public class RestClientException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public RestClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = "";
    }

    public RestClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = "";
    }

    public RestClientException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
