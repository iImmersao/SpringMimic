package com.iimmersao.springmimic.server;

import com.iimmersao.springmimic.core.ConfigLoader;
import fi.iki.elonen.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler {
    private static final String basePath;

    static {
        try {
            basePath = new ConfigLoader("application.properties").get("static.path", "public");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static NanoHTTPD.Response serve(String uriPath) throws IOException {
        String safePath = sanitizePath(uriPath);
        Path filePath = Paths.get(basePath, safePath).normalize();

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            return null; // Not found
        }

        String mimeType = getMimeType(filePath.toString());
        byte[] content = Files.readAllBytes(filePath);

        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mimeType, new ByteArrayInputStream(content), content.length);
    }

    private static String sanitizePath(String uri) {
        // Prevent directory traversal
        uri = uri.replace("..", "").replace("\\", "/");
        return uri.startsWith("/") ? uri.substring(1) : uri;
    }

    private static String getMimeType(String filename) {
        if (filename.endsWith(".html")) return "text/html";
        if (filename.endsWith(".css")) return "text/css";
        if (filename.endsWith(".js")) return "application/javascript";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
