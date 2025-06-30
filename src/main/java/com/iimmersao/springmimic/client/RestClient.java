package com.iimmersao.springmimic.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iimmersao.springmimic.annotations.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class RestClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RestClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public <T> T get(String url, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), responseType);
    }

    public <T> T post(String url, Object requestBody, Class<T> responseType) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), responseType);
    }

    // Optionally: add put(), patch(), delete() later
}
