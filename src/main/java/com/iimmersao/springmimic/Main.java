package com.iimmersao.springmimic;

import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ComponentScanner;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.server.WebServer;
import com.iimmersao.springmimic.web.Router;
import fi.iki.elonen.NanoHTTPD;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            // Load configuration from application.properties
            ConfigLoader config = new ConfigLoader("application.properties");

            System.out.println("Loaded config!");

            // Scan for @Controller and @Component classes
            ComponentScanner scanner = new ComponentScanner("com.iimmersao.springmimic"); // use your root package
            var componentClasses = scanner.scan();

            // Build application context (handles DI and @Value injection)
            ApplicationContext context = new ApplicationContext(componentClasses, config);

            // Register routes from all controller beans
            Router router = new Router();
            router.registerControllers(context.getAllBeans());

            // Start web server
            int port = Integer.parseInt(config.get("app.port"));
            WebServer server = new WebServer(port, router);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            System.out.println("Server started at http://localhost:" + port);


            System.out.println("Type 'exit' to stop the server.");

            /*
            try (Scanner console = new Scanner(System.in)) {
                while (true) {
                    if ("exit".equalsIgnoreCase(console.nextLine())) {
                        break;
                    }
                }
            }
            // Explicit shutdown
            context.shutdown();
            System.out.println("Server stopped cleanly.");
             */

            System.out.println("Press Ctrl+C to stop.");

            // Hook into shutdown to run @PreDestroy methods
            Runtime.getRuntime().addShutdownHook(new Thread(context::shutdown));

            // Block main thread so the app stays running
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
