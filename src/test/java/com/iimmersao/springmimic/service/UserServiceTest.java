package com.iimmersao.springmimic.service;

import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.H2DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.model.UserDTO;
import com.iimmersao.springmimic.routing.Port;
import com.iimmersao.springmimic.routing.Router;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class UserServiceTest {

    static int port = 9999;

    @Inject
    private static UserService userService;

    @BeforeAll
    static void createService() throws Exception {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        ConfigLoader config = new ConfigLoader();
        context.registerBean(ConfigLoader.class, config);

        // Create the appropriate DatabaseClient
        DatabaseClient databaseClient;
        String dbType = config.get("db.type", "mysql").toLowerCase();
        switch (dbType) {
            case "mongo", "mongodb" -> databaseClient = new MongoDatabaseClient(config);
            case "mysql" -> databaseClient = new MySqlDatabaseClient(config);
            case "h2" -> databaseClient = new H2DatabaseClient(config);
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

        context.registerBean(ApplicationContext.class, realContext); // Move to constructor?
        context.addComponents(realContext);
        context.injectDependencies();

        userService = context.getBean(UserService.class);
    }

    @Test
    void testStarts() {
        System.out.println("Test started");
        Optional<UserDTO> user = userService.findById("2405");
        if (user.isPresent()) {
            System.out.println("Found user");
        } else {
            System.out.println("Failed to find user");
        }
    }
}
