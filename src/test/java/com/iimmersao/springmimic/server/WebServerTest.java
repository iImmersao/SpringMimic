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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class WebServerTest {

    static WebServer server;
    static int port = 9999;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws Exception {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        context.initialize();
        context.injectDependencies();

        Router router = new Router();
        router.registerControllers(context.getControllers());
        ApplicationContext realContext = new ApplicationContext("com.iimmersao.springmimic");
        realContext.registerBean(Router.class, router);
        ConfigLoader config = new ConfigLoader("application.properties");
        realContext.registerBean(ConfigLoader.class, config);
        // Create the appropriate DatabaseClient
        DatabaseClient databaseClient;
        String dbType = config.get("db.type", "mysql").toLowerCase();
        switch (dbType) {
            case "mongo", "mongodb" -> databaseClient = new MongoDatabaseClient(config);
            case "mysql" -> databaseClient = new MySqlDatabaseClient(config);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        realContext.registerBean(DatabaseClient.class, databaseClient);
        Port portToUse = new Port(config.getInt("server.port", port));
        realContext.registerBean(Port.class, portToUse);
        RestClient restClient = new RestClient();
        realContext.registerBean(RestClient.class, restClient);
        realContext.initialize();
        realContext.injectDependencies();
        //server = new WebServer(portToUse, router);
        server = realContext.getBean(WebServer.class);
        server.start(1000, false);

        // Give the server a moment to bind the port
        Thread.sleep(500);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    void shouldHandleGetRequest() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/echo/hello").openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();

        assertEquals(200, responseCode);
        assertEquals("\"Echo: hello\"", response);
    }

    @Test
    void shouldHandlePostJson() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/json").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String body = "{\"name\":\"Philip\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        int responseCode = conn.getResponseCode();
        String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();

        assertEquals(200, responseCode);
        assertEquals("\"Received: Philip\"", response);
    }

    @Test
    void shouldHandleQueryParamsCorrectly() throws Exception {
        String url = "http://localhost:" + port + "/user/details?id=123&verbose=true&max=100";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();

        assertEquals(200, responseCode);
        assertEquals("\"Details for ID=123, verbose=true\"", response);
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
        String url = "http://localhost:" + port + "/posts/42/comments/7";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();

        assertEquals(200, responseCode);
        assertEquals("\"Post=42, Comment=7\"", response);
    }

    @Test
    void shouldReturnNotFoundForUnknownRoute() throws Exception {
        String url = "http://localhost:" + port + "/not-a-route";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();

        assertEquals(404, responseCode);
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
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/json").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String badJson = "{\"name\": }";  // malformed JSON
        try (OutputStream os = conn.getOutputStream()) {
            os.write(badJson.getBytes());
        }

        int responseCode = conn.getResponseCode();
        String response = new Scanner(conn.getErrorStream()).useDelimiter("\\A").next();

        assertEquals(400, responseCode);
        assertTrue(response.contains("Invalid request body"), response);
    }

    @Test
    void shouldReturnMethodNotAllowedForWrongHttpMethod() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/json").openConnection();
        conn.setRequestMethod("GET"); // Should be POST

        int responseCode = conn.getResponseCode();

        assertEquals(404, responseCode); // Framework uses 404 for method mismatch
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
