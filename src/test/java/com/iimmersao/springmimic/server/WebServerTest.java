package com.iimmersao.springmimic.server;

import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.routing.Port;
import com.iimmersao.springmimic.routing.Router;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings(value = "unused")
class WebServerTest {

    static WebServer server;
    final static int port = 9999;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws Exception {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        ConfigLoader config = new ConfigLoader();
        context.registerBean(ConfigLoader.class, config);

        // Create the appropriate DatabaseClient
        DatabaseClient databaseClient;
        String dbType = config.get("db.type", "mysql").toLowerCase();
        switch (dbType) {
            case "mongo", "mongodb" -> databaseClient = new MongoDatabaseClient(config);
            case "mysql" -> databaseClient = new MySqlDatabaseClient(config);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        context.registerDatabaseBean(DatabaseClient.class, databaseClient);
        context.initialize(null);

        ApplicationContext realContext = new ApplicationContext("com.iimmersao.springmimic");
        realContext.registerBean(ConfigLoader.class, config);

        realContext.registerDatabaseBean(DatabaseClient.class, databaseClient);
        realContext.registerBean(ApplicationContext.class, context); // Move to constructor?
        Port portToUse = new Port(config.getInt("server.port", port));
        realContext.registerBean(Port.class, portToUse);
        RestClient restClient = new RestClient();
        realContext.registerBean(RestClient.class, restClient);
        realContext.initialize(null);
        Router router = realContext.getBean(Router.class);
        router.registerControllers(context.getControllers());
        realContext.injectDependencies();

        context.registerBean(ApplicationContext.class, realContext);
        context.addComponents(realContext);
        context.injectDependencies();

        server = context.getBean(WebServer.class);
        server.start(1000, false);

        // Give the server a moment to bind the port
        Thread.sleep(500);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    private HttpRequest createRequest(String path, String method, String contentType, String body) {
        URI uri = URI.create(path);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri);

        if (contentType != null) {
            requestBuilder.header("Content-Type", contentType);
        }

        if ("GET".equals(method)) {
            requestBuilder.method(method.toUpperCase(), HttpRequest.BodyPublishers.noBody());
        }

        if ("POST".equals(method) && body != null) {
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
        }

        return requestBuilder.build();
    }

    @Test
    void shouldHandleGetRequest() throws Exception {
        HttpRequest request = createRequest("http://localhost:" + port + "/echo/hello", "GET",
                null, null);
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("Echo: hello", response.body());
    }

    @Test
    void shouldHandlePostJson() throws Exception {
        String body = "{\"name\":\"Philip\"}";
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = createRequest("http://localhost:" + port + "/json", "POST",
                "application/json", body);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        int responseCode = response.statusCode();
        String responseBody = response.body();
        String contentType = response.headers().firstValue("Content-Type").orElse("");

        assertEquals(200, responseCode);
        assertEquals("text/plain", contentType);
        assertEquals("Received: Philip", responseBody);
    }

    @Test
    void shouldHandleQueryParamsCorrectly() throws Exception {
        String body = "{\"name\":\"Philip\"}";
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = createRequest("http://localhost:" + port + "/user/details?id=123&verbose=true&max=100",
                "GET","application/json", body);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        int responseCode = response.statusCode();
        String responseBody = response.body();
        String contentType = response.headers().firstValue("Content-Type").orElse("");

        assertEquals(200, responseCode);
        assertEquals("text/plain", contentType);
        assertEquals("Details for ID=123, verbose=true", responseBody);
    }

    @Test
    void shouldHandleMissingOptionalQueryParam() throws Exception {
        // Arrange
        HttpClient client = HttpClient.newHttpClient();
        String url = "http://localhost:" + port + "/user/details?id=999&max=100";
        URI uri = URI.create(url);  // No ?verbose param

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        // Act
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert
        assertEquals(200, response.statusCode(), "Expected status 200 OK");
        assertTrue(response.body().contains("Details for ID=999"), "Expected response to include user ID");
    }

    @Test
    void shouldHandleMultiplePathVariables() throws Exception {
        HttpRequest request = createRequest("http://localhost:" + port + "/posts/42/comments/7", "GET",
                null, null);
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("Post=42, Comment=7", response.body());
    }

    @Test
    void shouldReturnNotFoundForUnknownRoute() throws Exception {
        HttpRequest request = createRequest("http://localhost:" + port + "/not-a-route", "GET",
                null, null);
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void shouldReturnBadRequestForInvalidBooleanParam() throws Exception {
        // Arrange
        HttpClient client = HttpClient.newHttpClient();
        String url = "http://localhost:" + port + "/user/details?id=55&verbose=maybe&max=100";
        URI uri = URI.create(url);  // No ?verbose param

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        // Act
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert
        assertEquals(400, response.statusCode(), "Expected status 400 OK");
        assertTrue(response.body().contains("Invalid value for request parameter 'verbose'"),
                "Expected response to give error about invalid Boolean value");
    }

    @Test
    void shouldReturnBadRequestForMissingRequiredParam() throws Exception {
        // Arrange
        HttpClient client = HttpClient.newHttpClient();
        String url = "http://localhost:" + port + "/user/details?verbose=true"; // missing 'id'
        URI uri = URI.create(url);  // No ?verbose param

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        // Act
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert
        assertEquals(400, response.statusCode(), "Expected status 400 OK");
        assertTrue(response.body().contains("Missing required request parameter"),
                "Expected response to give error about invalid Boolean value");
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        String body = "{\"name\": }";
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = createRequest("http://localhost:" + port + "/json", "POST",
                "application/json", body);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        int responseCode = response.statusCode();
        String responseBody = response.body();
        String contentType = response.headers().firstValue("Content-Type").orElse("");

        assertEquals(400, responseCode);
        assertTrue(responseBody.contains("Invalid request body"));
    }

    @Test
    void shouldReturnMethodNotAllowedForWrongHttpMethod() throws Exception {
        HttpRequest request = createRequest("http://localhost:" + port + "/json", "GET",
                null, null);
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void shouldHandlePutRequest() throws Exception {
        String body = "{\"name\": \"Updated Alice\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/users/123"))
                .method("PUT", HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Updated user 123"));
    }

    @Test
    void shouldHandlePatchRequest() throws Exception {
        String body = "{\"name\": \"Patched Alice\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/users/123"))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Patched user 123"));
    }

    @Test
    void shouldHandleDeleteRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/users/123"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Deleted user 123"));
    }
}
