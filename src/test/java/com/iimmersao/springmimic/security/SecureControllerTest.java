package com.iimmersao.springmimic.security;

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

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(value = "unused")
public class SecureControllerTest {

    private static WebServer server;
    private static final int port = 8081; // Use a unique port if your main app uses 8080

    @BeforeAll
    static void setUp() throws Exception {
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
    void tearDown() {
        server.stop();
        System.out.println("Test server stopped");
    }

    private HttpRequest createRequest(String path, String method, String authHeader) {
        URI uri = URI.create("http://localhost:" + port + path);
        Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .method(method.toUpperCase(), HttpRequest.BodyPublishers.noBody());

        if (authHeader != null) {
            requestBuilder.header("Authorization", authHeader);
        }

        return requestBuilder.build();
    }

    private String encodeBasicAuth(String username, String password) {
        String creds = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes());
    }

    @Test
    void shouldAllowAccessWithValidCredentials() throws Exception {
        HttpRequest request = createRequest("/secure", "GET", encodeBasicAuth("user", "user123"));
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("authenticated"));
    }

    @Test
    void shouldDenyAccessWithoutAuthHeader() throws Exception {
        HttpRequest request = createRequest("/secure", "GET", null);
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().toLowerCase().contains("missing or invalid authorization header"));
    }

    @Test
    void shouldDenyAccessWithInvalidCredentials() throws Exception {
        HttpRequest request = createRequest("/secure", "GET", encodeBasicAuth("bad", "badpass"));
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().toLowerCase().contains("unauthorized"));
    }

    @Test
    void shouldDenyAccessToAdminEndpointForUserRole() throws Exception {
        HttpRequest request = createRequest("/admin", "GET", encodeBasicAuth("user", "user123"));
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
        assertTrue(response.body().toLowerCase().contains("forbidden"));
    }

    @Test
    void shouldAllowAccessToAdminEndpointWithAdminRole() throws Exception {
        HttpRequest request = createRequest("/admin", "GET", encodeBasicAuth("admin", "admin123"));
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().toLowerCase().contains("admin"));
    }
}
