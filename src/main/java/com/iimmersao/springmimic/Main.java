package com.iimmersao.springmimic;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.server.WebServer;
import com.iimmersao.springmimic.routing.Router;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/*
public class Main {
    public static void main(String[] args) {
        String basePackage = "com.iimmersao.springmimic"; // Replace with your base package
        ApplicationContext context = new ApplicationContext(basePackage);

        Router router = new Router();
        router.registerControllers(context.getControllers());

        // Decide database type
        String dbType = ConfigLoader.get("db.type", "mysql");

        DatabaseClient dbClient;
        if ("mongo".equalsIgnoreCase(dbType)) {
            dbClient = new MongoDatabaseClient();
        } else if ("mysql".equalsIgnoreCase(dbType)) {
            dbClient = new MySqlDatabaseClient();
        } else {
            throw new RuntimeException("Unsupported db.type: " + dbType);
        }
        context.registerBean(DatabaseClient.class, dbClient);

        int port = ConfigLoader.getInt("server.port", 8080);
        WebServer server = new WebServer(port, router);
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("Server started at http://localhost:" + port);
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
 */

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
