package com.iimmersao.springmimic;

import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.server.WebServer;
import com.iimmersao.springmimic.routing.Router;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        String basePackage = "com.iimmersao.springmimic"; // Replace with your base package
        ApplicationContext context = new ApplicationContext(basePackage);

        Router router = new Router();
        Set<Object> controllers = context.getControllers();
        router.registerControllers(controllers);

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