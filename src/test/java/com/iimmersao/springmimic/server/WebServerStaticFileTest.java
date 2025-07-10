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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServerStaticFileTest {

    private static Path tempDir;
    private static WebServer server;
    private static final int port = 8089;

    @BeforeAll
    static void setUp() throws Exception {
        tempDir = Files.createTempDirectory("static-test");
        // Create some static files
        Files.writeString(tempDir.resolve("index.html"), "<h1>Hello Index</h1>");
        Files.writeString(tempDir.resolve("about.html"), "<p>About Page</p>");

        // Inject ConfigLoader with custom static.dir
        ConfigLoader config = new ConfigLoader("application.properties") {
            @Override
            public String get(String key) {
                if ("static.dir".equals(key)) {
                    return tempDir.toAbsolutePath().toString();
                } else {
                    return super.get(key);
                }
                //return null;
            }
        };

        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic");
        context.registerBean(ConfigLoader.class, config);
        // Create the appropriate DatabaseClient
        DatabaseClient databaseClient;
        String dbType = config.get("db.type", "mysql").toLowerCase();
        switch (dbType) {
            case "mongo", "mongodb" -> databaseClient = new MongoDatabaseClient(config);
            case "mysql" -> databaseClient = new MySqlDatabaseClient(config);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        context.registerBean(DatabaseClient.class, databaseClient);
        context.registerBean(ApplicationContext.class, context); // Move to constructor?
        Port portToUse = new Port(config.getInt("server.port", port));
        context.registerBean(Port.class, portToUse);
        RestClient restClient = new RestClient();
        context.registerBean(RestClient.class, restClient);
        context.initialize(null);

        Router router = context.getBean(Router.class);
        router.registerControllers(context.getControllers());

        context.injectDependencies();

        server = context.getBean(WebServer.class);
        server.start(port);
    }

    @AfterAll
    static void tearDown() {
        server.stop();
    }

    @Test
    void shouldServeExistingStaticFile() throws Exception {
        HttpResponse<String> response = sendGet("/about.html");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("About Page"));
    }

    @Test
    void shouldServeIndexHtmlAtRoot() throws Exception {
        HttpResponse<String> response = sendGet("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Welcome to My Custom Web Framework"));
    }

    @Test
    void shouldReturn404ForMissingStaticFile() throws Exception {
        HttpResponse<String> response = sendGet("/missing.html");
        assertEquals(404, response.statusCode());
    }

    @Test
    void shouldPreventDirectoryTraversal() throws Exception {
        HttpResponse<String> response = sendGet("/../somefile.txt");
        assertEquals(404, response.statusCode());
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        URI uri = new URI("http://localhost:" + port + path);
        HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
