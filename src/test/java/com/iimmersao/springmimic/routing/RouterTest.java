package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.H2DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings(value = "unused")
public class RouterTest {

    static class TestController {
        @GetMapping("/users/{id}")
        public String getUser(String id) {
            return "User ID: " + id;
        }

        @GetMapping("/posts/{postId}/comments/{commentId}")
        public String getComment(String postId, String commentId) {
            return "Post " + postId + ", Comment " + commentId;
        }
    }

    private Router router;

    @BeforeEach
    void setUp() {
        ConfigLoader config = new ConfigLoader();
        String dbType = config.get("db.type", "mysql").toLowerCase();

        // Create the appropriate DatabaseClient
        DatabaseClient databaseClient;
        switch (dbType) {
            case "mongo", "mongodb" -> databaseClient = new MongoDatabaseClient(config);
            case "mysql" -> databaseClient = new MySqlDatabaseClient(config);
            case "h2" -> databaseClient = new H2DatabaseClient(config);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic");
        context.registerBean(ApplicationContext.class, context); // Move to constructor?
        Port port = new Port(config.getInt("server.port", 8080));
        context.registerBean(Port.class, port);
        context.registerBean(DatabaseClient.class, databaseClient);
        context.registerBean(ConfigLoader.class, config);
        RestClient restClient = new RestClient();
        context.registerBean(RestClient.class, restClient);
        context.initialize(null);
        router = context.getBean(Router.class);
        context.injectDependencies();
        Set<Object> controllers = Set.of(new TestController());
        router.registerControllers(controllers);
    }

    @Test
    void shouldMatchPathWithSingleVariable() {
        String method = "GET";
        String uri = "/users/abc123";

        RouteMatch match = router.findHandler("GET", uri);
        assertNotNull(match, "Expected route to be found for /users/{id}");
    }

    @Test
    void shouldMatchPathWithMultipleVariables() {
        String method = "GET";
        String uri = "/posts/99/comments/123";

        RouteMatch match = router.findHandler("GET", uri);
        assertNotNull(match, "Expected route to be found for /posts/{postId}/comments/{commentId}");
    }

    @Test
    void shouldNotMatchUnknownRoute() {
        String method = "GET";
        String uri = "/unknown/path";

        RouteMatch match = router.findHandler("GET", uri);
        assertNull(match, "Expected no route to be found for /unknown/path");
    }
}
