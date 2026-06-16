package com.iimmersao.springmimic;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.iimmersao.springmimic.annotations.ComponentScan;
import com.iimmersao.springmimic.client.RestClient;
import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.H2DatabaseClient;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;
import com.iimmersao.springmimic.routing.Port;
import com.iimmersao.springmimic.routing.Router;
import com.iimmersao.springmimic.server.WebServer;
import com.iimmersao.springmimic.transaction.JdbcTransactionManager;
import com.iimmersao.springmimic.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class SpringMimicApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringMimicApplicationRunner.class);

    private static volatile CountDownLatch shutdownSignal;

    public static void run(Class<?> applicationClass) {
        shutdownSignal = new CountDownLatch(1);
        DatabaseClient databaseClient = null;
        WebServer server = null;
        try {
            // Create the context for the user-level application
            String basePackage = getBasePackage(applicationClass);
            System.out.println("Base package is: " + basePackage);
            ApplicationContext context = new ApplicationContext(basePackage);

            // Load configuration
            ConfigLoader config = new ConfigLoader();
            context.registerBean(ConfigLoader.class, config);
            configureLogging(config);

            System.out.println("Setting up database access");
            DatabaseSetup databaseSetup = createDatabaseSetup(config);
            databaseClient = databaseSetup.databaseClient();
            TransactionManager transactionManager = databaseSetup.transactionManager();
            System.out.println("Set up DatabaseClient as: " + databaseClient.getClass().getName());
            context.registerDatabaseBean(DatabaseClient.class, databaseClient);
            if (transactionManager != null) {
                context.registerBean(TransactionManager.class, transactionManager);
            }
            System.out.println("Initialising application context");
            context.initialize(null);
            System.out.println("Initialised application context");

            // Create application context and manually register the client
            ApplicationContext springMimicContext = new ApplicationContext("com.iimmersao.springmimic");
            springMimicContext.registerBean(ConfigLoader.class, config);

            springMimicContext.registerDatabaseBean(DatabaseClient.class, databaseClient);
            if (transactionManager != null) {
                springMimicContext.registerBean(TransactionManager.class, transactionManager);
            }
            springMimicContext.registerBean(ApplicationContext.class, context);

            Port port = new Port(config.getInt("server.port", 8080));
            springMimicContext.registerBean(Port.class, port);
            RestClient restClient = new RestClient(config);
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
            server = context.getBean(WebServer.class);
            server.start();

            System.out.println("Server started on port " + port);
            log.info("Application started with database: {}", config.get("db.type"));
            log.info("Environment: {}", config.get("env", "development"));
            System.out.println("SpringMimic application " + config.get("server.name") + " started");

            waitForStopSignal();
        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            log.error(Arrays.toString(e.getStackTrace()));
        } finally {
            if (server != null) {
                server.stop();
            }
            closeQuietly(databaseClient);
        }
    }

    public void stop() {
        CountDownLatch signal = shutdownSignal;
        if (signal != null) {
            signal.countDown();
        }
    }

    private static void waitForStopSignal() {
        CountDownLatch signal = shutdownSignal;
        if (signal == null) {
            return;
        }
        try {
            signal.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(Object resource) {
        if (resource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.warn("Failed to close resource {}", resource.getClass().getName(), e);
            }
        }
    }

    private static void configureLogging(ConfigLoader config) {
        String level = config.get("logging.level", "INFO").trim().toUpperCase(Locale.ROOT);
        String output = loggingOutput(config);
        String outputFile = config.get("logging.file", "logs/app.log").trim();

        System.setProperty("LOG_LEVEL", level);
        System.setProperty("LOG_FILE", outputFile);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(ch.qos.logback.classic.Level.toLevel(level, ch.qos.logback.classic.Level.INFO));

        configureFileAppender(rootLogger, outputFile);
        configureAppender(rootLogger, "CONSOLE", output.equals("console") || output.equals("both"));
        configureAppender(rootLogger, "FILE", output.equals("file") || output.equals("both"));
    }

    private static String loggingOutput(ConfigLoader config) {
        String output = config.get("logging.output", "both").trim().toLowerCase(Locale.ROOT);
        return switch (output) {
            case "console", "file", "both" -> output;
            default -> "both";
        };
    }

    private static void configureFileAppender(ch.qos.logback.classic.Logger rootLogger, String outputFile) {
        Appender<ILoggingEvent> appender = rootLogger.getAppender("FILE");
        if (appender instanceof FileAppender<ILoggingEvent> fileAppender) {
            fileAppender.stop();
            fileAppender.setFile(outputFile);
            fileAppender.start();
        }
    }

    private static void configureAppender(
            ch.qos.logback.classic.Logger rootLogger,
            String appenderName,
            boolean enabled) {
        Appender<ILoggingEvent> appender = rootLogger.getAppender(appenderName);
        if (appender == null) {
            return;
        }
        if (enabled) {
            if (!rootLogger.isAttached(appender)) {
                rootLogger.addAppender(appender);
            }
        } else {
            rootLogger.detachAppender(appenderName);
        }
    }

    private static DatabaseSetup createDatabaseSetup(ConfigLoader config) {
        String dbType = config.get("db.type", "mysql").toLowerCase();
        return switch (dbType) {
            case "mongo", "mongodb" -> new DatabaseSetup(new MongoDatabaseClient(config), null);
            case "mysql" -> createMySqlSetup(config);
            case "h2" -> createH2Setup(config);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        };
    }

    private static DatabaseSetup createMySqlSetup(ConfigLoader config) {
        JdbcConnectionProvider rawProvider = new DriverManagerConnectionProvider(
                config.get("database.url"),
                config.get("database.username"),
                config.get("database.password")
        );
        DatabaseClient databaseClient = new MySqlDatabaseClient(new TransactionAwareConnectionProvider(rawProvider));
        return new DatabaseSetup(databaseClient, new JdbcTransactionManager(rawProvider, transactionTimeout(config)));
    }

    private static DatabaseSetup createH2Setup(ConfigLoader config) {
        JdbcConnectionProvider rawProvider = new DriverManagerConnectionProvider(
                config.get("h2.url"),
                config.get("h2.username"),
                config.get("h2.password")
        );
        DatabaseClient databaseClient = new H2DatabaseClient(config, new TransactionAwareConnectionProvider(rawProvider));
        return new DatabaseSetup(databaseClient, new JdbcTransactionManager(rawProvider, transactionTimeout(config)));
    }

    private static int transactionTimeout(ConfigLoader config) {
        return config.getInt("springmimic.transaction.default-timeout-seconds", -1);
    }

    private static String getBasePackage(Class<?> mainClass) {
        ComponentScan scanAnnotation = mainClass.getAnnotation(ComponentScan.class);
        if (scanAnnotation != null) {
            return scanAnnotation.value();
        }
        return mainClass.getPackageName(); // fallback
    }

    private record DatabaseSetup(DatabaseClient databaseClient, TransactionManager transactionManager) {}
}
