package com.iimmersao.springmimic;

import ch.qos.logback.classic.LoggerContext;
import com.iimmersao.springmimic.annotations.ComponentScan;
import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ComponentScanner;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.H2DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.routing.Port;
import com.iimmersao.springmimic.routing.Router;
import com.iimmersao.springmimic.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class SpringMimicApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringMimicApplicationRunner.class);

    private static boolean running;

    public static void run(Class<?> applicationClass) {
        try {
            // Create the context for the user-level application
            String basePackage = getBasePackage(applicationClass);
            System.out.println("Base package is: " + basePackage);
            ApplicationContext context = new ApplicationContext(basePackage);

            // Load configuration
            ConfigLoader config = new ConfigLoader();
            context.registerBean(ConfigLoader.class, config);
            String level = config.get("logging.level");
            if (level != null) System.setProperty("LOG_LEVEL", level.trim());

            String outputFile = config.get("logging.file");
            if (outputFile != null) System.setProperty("LOG_FILE", outputFile.trim());

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger("root")
                    .setLevel(ch.qos.logback.classic.Level.convertAnSLF4JLevel(Level.valueOf(level)));

            System.out.println("Setting up database access");
            // Create the appropriate DatabaseClient
            DatabaseClient databaseClient;
            String dbType = config.get("db.type", "mysql").toLowerCase();
            switch (dbType) {
                case "mongo", "mongodb" -> databaseClient = new MongoDatabaseClient(config);
                case "mysql" -> databaseClient = new MySqlDatabaseClient(config);
                case "h2" -> databaseClient = new H2DatabaseClient(config);
                default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
            }
            System.out.println("Set up DatabaseClient as: " + databaseClient.getClass().getName());
            context.registerDatabaseBean(DatabaseClient.class, databaseClient);
            System.out.println("Initialising application context");
            context.initialize(null);
            System.out.println("Initialised application context");

            // Create application context and manually register the client
            ApplicationContext springMimicContext = new ApplicationContext("com.iimmersao.springmimic");
            springMimicContext.registerBean(ConfigLoader.class, config);

            springMimicContext.registerDatabaseBean(DatabaseClient.class, databaseClient);
            springMimicContext.registerBean(ApplicationContext.class, context);

            Port port = new Port(config.getInt("server.port", 8080));
            springMimicContext.registerBean(Port.class, port);
            RestClient restClient = new RestClient();
            springMimicContext.registerBean(RestClient.class, restClient);
            springMimicContext.initialize(null);
            System.out.println("Initialised SpringMimic context");
            Router router = springMimicContext.getBean(Router.class);
            router.registerControllers(context.getControllers());
            springMimicContext.injectDependencies();
            System.out.println("Injected SpringMimic dependencies");

            context.registerBean(ApplicationContext.class, springMimicContext);
            context.addComponents(springMimicContext);
            System.out.println("Added SpringMimic components to application context");
            context.injectDependencies();
            System.out.println("Injected application dependencies");

            // Start the web server
            WebServer server = context.getBean(WebServer.class);
            server.start();

            System.out.println("Server started on port " + port);
            log.info("Application started with database: {}", config.get("db.type"));
            log.info("Environment: {}", config.get("env", "development"));
            // Allow main thread to be shut down by another thread.
            running = true;

            while (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }

    private static String getBasePackage(Class<?> mainClass) {
        ComponentScan scanAnnotation = mainClass.getAnnotation(ComponentScan.class);
        if (scanAnnotation != null) {
            return scanAnnotation.value();
        }
        return mainClass.getPackageName(); // fallback
    }
}
