package com.iimmersao.springmimic.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.iki.elonen.NanoHTTPD;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;

class RestClientTest {

    private static TestServer server;
    private static RestClient client;
    private static final int PORT = 8089;
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeAll
    static void setup() throws Exception {
        server = new TestServer(PORT);
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        client = new RestClient();
    }

    @AfterAll
    static void teardown() {
        server.stop();
    }

    // Simple DTO
    public static class User {
        public final String name;

        @JsonCreator
        public User(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    // === SUCCESS CASES ===

    @Test
    void testGet() {
        String result = client.get(BASE_URL + "/hello", String.class);
        assertEquals("Hello World", result);
    }

    @Test
    void testPost() {
        User user = new User("Alice");
        String result = client.post(BASE_URL + "/echo", user, String.class);
        assertTrue(result.contains("Alice"));
    }

    @Test
    void testPut() {
        User user = new User("Bob");
        String result = client.put(BASE_URL + "/update", user, String.class);
        assertTrue(result.contains("Bob"));
    }

    @Test
    void testPatch() {
        User user = new User("Charlie");
        String result = client.patch(BASE_URL + "/modify", user, String.class);
        assertTrue(result.contains("Charlie"));
    }

    @Test
    void testDelete() {
        String result = client.delete(BASE_URL + "/remove", String.class);
        assertEquals("Deleted", result);
    }

    // === ERROR CASES ===

    @Test
    void testNotFound() {
        RestClientException ex = assertThrows(RestClientException.class, () ->
                client.get(BASE_URL + "/notfound", String.class));
        assertEquals(404, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("HTTP 404"));
    }

    @Test
    void testInternalServerError() {
        RestClientException ex = assertThrows(RestClientException.class, () ->
                client.get(BASE_URL + "/error", String.class));
        assertEquals(500, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    // === MOCK SERVER ===

    private static class TestServer extends NanoHTTPD {
        public TestServer(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            Method method = session.getMethod();

            try {
                switch (uri) {
                    case "/hello":
                        return newFixedLengthResponse("Hello World");
                    case "/echo":
                        return withBodyEcho(session);
                    case "/update":
                        return withBodyEcho(session);
                    case "/modify":
                        return withBodyEcho(session);
                    case "/remove":
                        return newFixedLengthResponse("Deleted");
                    case "/notfound":
                        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
                    case "/error":
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
                    default:
                        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Unknown route");
                }
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Exception: " + e.getMessage());
            }
        }

        private Response withBodyEcho(IHTTPSession session) throws IOException {
            int contentLength = 0;
            String len = session.getHeaders().get("content-length");
            if (len != null) {
                contentLength = Integer.parseInt(len);
            }

            byte[] bodyBytes = session.getInputStream().readNBytes(contentLength);
            String body = new String(bodyBytes, StandardCharsets.UTF_8);

            return newFixedLengthResponse(Response.Status.OK, "application/json", body);
        }
    }
}
