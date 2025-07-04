package com.iimmersao.springmimic;

import ch.qos.logback.classic.LoggerContext;
import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.routing.Port;
import com.iimmersao.springmimic.server.WebServer;
import com.iimmersao.springmimic.routing.Router;
import com.iimmersao.springmimic.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // Load configuration
            ConfigLoader config = new ConfigLoader("application.properties");
            String level = config.get("logging.level");
            if (level != null) System.setProperty("LOG_LEVEL", level.trim());

            String outputFile = config.get("logging.file");
            if (outputFile != null) System.setProperty("LOG_FILE", outputFile.trim());
            //String logLevelStr = config.get("log.level", "INFO").toUpperCase();

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger("root")
                    .setLevel(ch.qos.logback.classic.Level.convertAnSLF4JLevel(Level.valueOf(level)));

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

            //int port = config.getInt("server.port", 8080);
            Port port = new Port(config.getInt("server.port", 8080));
            context.registerBean(Port.class, port);

            //WebServer server = new WebServer(port, router);
            context.initialize();
            Router router = new Router();
            router.registerControllers(context.getControllers());
            context.registerBean(Router.class, router);
            context.injectDependencies();
            // Start the web server
            WebServer server = context.getBean(WebServer.class);
            server.start();

            System.out.println("Server started on port " + port);
            log.info("Application started with database: {}", config.get("db.type"));
            log.info("Environment: {}", config.get("env", "development"));
            // Block main thread
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
