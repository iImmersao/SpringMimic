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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SecureControllerTest {

    private static WebServer server;
    private static final int port = 8081; // Use a unique port if your main app uses 8080

    @BeforeAll
    static void setUp() throws Exception {
        ApplicationContext realContext = new ApplicationContext("com.iimmersao.springmimic");
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
        realContext.registerBean(ApplicationContext.class, realContext); // Move to constructor?
        RestClient restClient = new RestClient();
        realContext.registerBean(RestClient.class, restClient);
        realContext.initialize();
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

    private HttpURLConnection createConnection(String path, String method, String authHeader) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        if (authHeader != null) {
            conn.setRequestProperty("Authorization", authHeader);
        }
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        Scanner scanner = new Scanner(
                (conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream())
        ).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private String encodeBasicAuth(String username, String password) {
        String creds = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes());
    }

    @Test
    void shouldAllowAccessWithValidCredentials() throws Exception {
        HttpURLConnection conn = createConnection("/secure", "GET", encodeBasicAuth("user", "user123"));
        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(200, responseCode);
        assertTrue(response.contains("authenticated"));
    }

    @Test
    void shouldDenyAccessWithoutAuthHeader() throws Exception {
        HttpURLConnection conn = createConnection("/secure", "GET", null);
        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(401, responseCode);
        assertTrue(response.toLowerCase().contains("missing or invalid authorization header"));
    }

    @Test
    void shouldDenyAccessWithInvalidCredentials() throws Exception {
        HttpURLConnection conn = createConnection("/secure", "GET", encodeBasicAuth("bad", "badpass"));
        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(401, responseCode);
        assertTrue(response.toLowerCase().contains("unauthorized"));
    }

    @Test
    void shouldDenyAccessToAdminEndpointForUserRole() throws Exception {
        HttpURLConnection conn = createConnection("/admin", "GET", encodeBasicAuth("user", "user123"));
        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(403, responseCode);
        assertTrue(response.toLowerCase().contains("forbidden"));
    }

    @Test
    void shouldAllowAccessToAdminEndpointWithAdminRole() throws Exception {
        HttpURLConnection conn = createConnection("/admin", "GET", encodeBasicAuth("admin", "admin123"));
        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(200, responseCode);
        assertTrue(response.toLowerCase().contains("admin"));
    }
}
