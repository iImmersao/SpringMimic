package com.iimmersao.springmimic;

import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.server.WebServer;
import com.iimmersao.springmimic.routing.Router;

public class Main {
    public static void main(String[] args) {
        try {
            // Load configuration
            ConfigLoader config = new ConfigLoader("application.properties");
            String dbType = config.get("db.type", "mysql").toLowerCase();

            // Create the appropriate DatabaseClient
            DatabaseClient databaseClient;
            switch (dbType) {
                case "mongo", "mongodb" -> databaseClient = new MongoDatabaseClient(config);
                case "mysql" -> databaseClient = new MySqlDatabaseClient(config);
                default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
            }

            // Create application context and manually register the client
            ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic");
            context.registerBean(DatabaseClient.class, databaseClient);
            context.registerBean(ConfigLoader.class, config);
            RestClient restClient = new RestClient();
            context.registerBean(RestClient.class, restClient);
            /*
            UserService userService = new UserService();
            context.registerBean(UserService.class, userService);
             */
            context.initialize();

            // Start the web server
            Router router = new Router();
            router.registerControllers(context.getControllers());
            int port = config.getInt("server.port", 8080);
            WebServer server = new WebServer(port, router);
            server.start();

            System.out.println("Server started on port " + port);
            // Block main thread
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
