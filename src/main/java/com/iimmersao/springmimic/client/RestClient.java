package com.iimmersao.springmimic.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.iimmersao.springmimic.annotations.Bean;

@Bean
public class RestClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> T get(String url, Class<T> responseType) {
        return sendRequest("GET", url, null, responseType);
    }

    public <T> T post(String url, Object body, Class<T> responseType) {
        return sendRequest("POST", url, body, responseType);
    }

    public <T> T put(String url, Object body, Class<T> responseType) {
        return sendRequest("PUT", url, body, responseType);
    }

    public <T> T patch(String url, Object body, Class<T> responseType) {
        return sendRequest("PATCH", url, body, responseType);
    }

    public <T> T delete(String url, Class<T> responseType) {
        return sendRequest("DELETE", url, null, responseType);
    }

    private <T> T sendRequest(String method, String urlStr, Object body, Class<T> responseType) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .header("Accept", "application/json");

            if (body != null) {
                String json = objectMapper.writeValueAsString(body);
                builder.method(method, HttpRequest.BodyPublishers.ofString(json));
                builder.header("Content-Type", "application/json");
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode >= 400) {
                throw new RestClientException("HTTP " + statusCode + ": " + response.body(), statusCode);
            }

            if (responseType == String.class) {
                return responseType.cast(response.body());
            }

            return objectMapper.readValue(response.body(), responseType);
        } catch (RestClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RestClientException("Failed to send HTTP " + method + " request to " + urlStr, e);
        }
    }
}
