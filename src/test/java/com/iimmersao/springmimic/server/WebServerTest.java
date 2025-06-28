package com.iimmersao.springmimic.server;

import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.routing.Router;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class WebServerTest {

    static WebServer server;
    static int port = 9999;

    @BeforeAll
    static void startServer() throws Exception {
        ApplicationContext context = new ApplicationContext("com.iimmersao.springmimic.testcomponents");
        Router router = new Router();
        router.registerControllers(context.getControllers());
        server = new WebServer(port, router);
        server.start(1000, false);

        // Give the server a moment to bind the port
        Thread.sleep(500);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    void shouldHandleGetRequest() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/echo/hello").openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();

        assertEquals(200, responseCode);
        assertEquals("Echo: hello", response);
    }

    @Test
    void shouldHandlePostJson() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/json").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String body = "{\"name\":\"Philip\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        int responseCode = conn.getResponseCode();
        String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();

        assertEquals(200, responseCode);
        assertEquals("Received: Philip", response);
    }
}
