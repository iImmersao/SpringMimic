package com.iimmersao.springmimic.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.core.ConfigLoader;

@Bean
public class RestClient {

    private final Map<String, String> defaultHeaders;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final int maxRetries;
    private final int retryDelayMillis;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RestClient() {
        this(null);
    }

    public RestClient(ConfigLoader config) {
        if (config == null) {
            this.defaultHeaders = new HashMap<>();
            this.connectTimeoutMillis = 5000;
            this.readTimeoutMillis = 5000;
            this.maxRetries = 0;
            this.retryDelayMillis = 1000;
        } else {
            this.defaultHeaders = config.getSubProperties("restclient.defaultHeaders.", Set.of("Accept", "Content-Type"));
            this.connectTimeoutMillis = config.getInt("restclient.connectTimeoutMillis", 5000);
            this.readTimeoutMillis = config.getInt("restclient.readTimeoutMillis", 5000);
            this.maxRetries = config.getInt("restclient.maxRetries", 0);
            this.retryDelayMillis = config.getInt("restclient.retryDelayMillis", 1000);
        }
    }

    // === GET ===
    public <T> T get(String url, Class<T> responseType) throws IOException {
        return get(url, responseType, null);
    }

    public <T> T get(String url, Class<T> responseType, Map<String, String> headers) throws IOException {
        RestResponse response = executeWithRetries("GET", url, null, mergeHeaders(headers));
        return deserialize(response.getBody(), responseType);
    }

    public RestResponse getRaw(String url) throws IOException {
        return getRaw(url, null);
    }

    public RestResponse getRaw(String url, Map<String, String> headers) throws IOException {
        return executeWithRetries("GET", url, null, mergeHeaders(headers));
    }

    // === POST ===
    public <T> T post(String url, Object requestBody, Class<T> responseType) throws IOException {
        return post(url, requestBody, responseType, null);
    }

    public <T> T post(String url, Object requestBody, Class<T> responseType, Map<String, String> headers) throws IOException {
        String body = serialize(requestBody);
        RestResponse response = executeWithRetries("POST", url, body, mergeHeaders(headers));
        return deserialize(response.getBody(), responseType);
    }

    public RestResponse postRaw(String url, Object requestBody) throws IOException {
        return postRaw(url, requestBody, null);
    }

    public RestResponse postRaw(String url, Object requestBody, Map<String, String> headers) throws IOException {
        String body = serialize(requestBody);
        return executeWithRetries("POST", url, body, mergeHeaders(headers));
    }

    // === PUT ===
    public <T> T put(String url, Object requestBody, Class<T> responseType) throws IOException {
        return put(url, requestBody, responseType, null);
    }

    public <T> T put(String url, Object requestBody, Class<T> responseType, Map<String, String> headers) throws IOException {
        String body = serialize(requestBody);
        RestResponse response = executeWithRetries("PUT", url, body, mergeHeaders(headers));
        return deserialize(response.getBody(), responseType);
    }

    public RestResponse putRaw(String url, Object requestBody) throws IOException {
        return putRaw(url, requestBody, null);
    }

    public RestResponse putRaw(String url, Object requestBody, Map<String, String> headers) throws IOException {
        String body = serialize(requestBody);
        return executeWithRetries("PUT", url, body, mergeHeaders(headers));
    }

    // === PATCH ===
    public <T> T patch(String url, Object requestBody, Class<T> responseType) throws IOException {
        return patch(url, requestBody, responseType, null);
    }

    public <T> T patch(String url, Object requestBody, Class<T> responseType, Map<String, String> headers) throws IOException {
        String body = serialize(requestBody);
        RestResponse response = executeWithRetries("PATCH", url, body, mergeHeaders(headers));
        return deserialize(response.getBody(), responseType);
    }

    public RestResponse patchRaw(String url, Object requestBody) throws IOException {
        return patchRaw(url, requestBody, null);
    }

    public RestResponse patchRaw(String url, Object requestBody, Map<String, String> headers) throws IOException {
        String body = serialize(requestBody);
        return executeWithRetries("PATCH", url, body, mergeHeaders(headers));
    }

    // === DELETE ===
    public <T> T delete(String url, Class<T> responseType) throws IOException {
        return delete(url, responseType, null);
    }

    public <T> T delete(String url, Class<T> responseType, Map<String, String> headers) throws IOException {
        RestResponse response = executeWithRetries("DELETE", url, null, mergeHeaders(headers));
        return deserialize(response.getBody(), responseType);
    }

    public RestResponse deleteRaw(String url) throws IOException {
        return deleteRaw(url, null);
    }

    public RestResponse deleteRaw(String url, Map<String, String> headers) throws IOException {
        return executeWithRetries("DELETE", url, null, mergeHeaders(headers));
    }

    // === Internals ===

    private <T> T deserialize(String body, Class<T> type) throws IOException {
        if (type == String.class) {
            // no JSON parsing needed — just return raw string
            @SuppressWarnings("unchecked")
            T result = (T) body;
            return result;
        }
        return objectMapper.readValue(body, type);
    }

    private String serialize(Object obj) throws IOException {
        return objectMapper.writeValueAsString(obj);
    }

    private Map<String, String> mergeHeaders(Map<String, String> requestHeaders) {
        Map<String, String> merged = new HashMap<>(defaultHeaders);
        if (requestHeaders != null) {
            merged.putAll(requestHeaders);
        }
        return merged;
    }

    private RestResponse executeWithRetries(String method, String url, String body, Map<String, String> headers) throws IOException {
        int attempt = 0;
        while (true) {
            try {
                return executeRequest(method, url, body, headers);
            } catch (IOException e) {
                attempt++;
                if (attempt > maxRetries) throw e;
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException interrupt) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", interrupt);
                }
            }
        }
    }

    private RestResponse executeRequest(String method, String url, String body, Map<String, String> headers) throws IOException {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                    .build();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(readTimeoutMillis));

            // Configure method
            switch (method.toUpperCase()) {
                case "GET", "DELETE" -> builder.method(method, HttpRequest.BodyPublishers.noBody());
                case "POST", "PUT", "PATCH" -> {
                    if (body != null) {
                        builder.method(method, HttpRequest.BodyPublishers.ofString(body));
                    } else {
                        builder.method(method, HttpRequest.BodyPublishers.noBody());
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            // Set headers
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String responseBody = response.body();

            // ✅ Throw exception on non-2xx responses
            if (status < 200 || status >= 300) {
                throw new RestClientException("HTTP " + status + ": " + responseBody, status, responseBody);
            }

            return new RestResponse(status, responseBody);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        } catch (HttpTimeoutException e) {
            throw new IOException("Request timed out", e);
        }
    }
}
