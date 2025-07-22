package com.iimmersao.springmimic.annotations;

import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.routing.Port;
import com.iimmersao.springmimic.routing.Router;
import com.iimmersao.springmimic.server.WebServer;
import org.junit.jupiter.api.*;

import java.net.http.*;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings(value = "unused")
class ResponseBodyTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static HttpClient client;
    private static WebServer server;
    private static final int port = 8080; // Use a unique port if your main app uses 8080

    @BeforeAll
    static void setUp() throws Exception {
        client = HttpClient.newHttpClient();
        ApplicationContext realContext = new ApplicationContext("com.iimmersao.springmimic");
        ConfigLoader config = new ConfigLoader();
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
        realContext.registerBean(ApplicationContext.class, realContext); // Move to constructor?
        RestClient restClient = new RestClient();
        realContext.registerBean(RestClient.class, restClient);
        realContext.initialize(null);
        Router router = realContext.getBean(Router.class);
        router.registerControllers(realContext.getControllers());
        realContext.injectDependencies();
        server = realContext.getBean(WebServer.class);
        server.start(1000, false);

        // Give the server a moment to bind the port
        Thread.sleep(500);
    }

    @AfterAll
    static void tearDown() {
        server.stop();
        System.out.println("Test server stopped");
    }

    @Test
    void testPlainTextResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/plaintext/john"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("hello john", response.body());
    }

    @Test
    void testJsonResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/json"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));

        String expectedJson = "{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\"}";
        assertEquals(expectedJson, response.body());
    }

    @Test
    void testWithoutResponseBodyAnnotation() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/noannotation"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("Content-Type").orElse(""));

        String expectedJson = "H2User{id=42, username='Alice', email='alice@example.com'}";
        assertEquals(expectedJson, response.body());
    }

    @Test
    void testRestPlainTextResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restplaintext/john"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("hello john", response.body());
    }

    @Test
    void testRestJsonResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restjson"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));

        String expectedJson = "{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\"}";
        assertEquals(expectedJson, response.body());
    }

    @Test
    void testRestWithoutResponseBodyAnnotation() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restnoannotation"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));

        String expectedJson = "{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\"}";
        assertEquals(expectedJson, response.body());
    }

    @Test
    void testRespBodyPlainTextResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/respbodyplaintext/john"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("hello john", response.body());
    }

    @Test
    void testRespBodyJsonResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/respbodyjson"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));

        String expectedJson = "{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\"}";
        assertEquals(expectedJson, response.body());
    }

    @Test
    void testRespBodyWithoutResponseBodyAnnotation() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/respbodynoannotation"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));

        String expectedJson = "{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\"}";
        assertEquals(expectedJson, response.body());
    }
}
