package com.iimmersao.springmimic.server;

import com.iimmersao.springmimic.Main;
import com.iimmersao.springmimic.annotations.Component;
import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.core.ExceptionHandler;
import com.iimmersao.springmimic.exceptions.RouteNotFoundException;
import com.iimmersao.springmimic.routing.Port;
import com.iimmersao.springmimic.routing.Router;
import com.iimmersao.springmimic.routing.RouteMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Component
public class WebServer extends NanoHTTPD {

    private static final Logger log = LoggerFactory.getLogger(WebServer.class);

    @Inject
    private final Router router;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private File staticDir;

    @Inject
    private ConfigLoader configLoader;

    public WebServer(Port port, Router router) {
        super(port.getPortNo());
        this.router = router;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().name();

        // Automatically load from config
        if (staticDir == null) {
            String staticPath = configLoader.get("static.path");
            staticDir = staticPath != null
                    ? Paths.get("").toAbsolutePath().resolve(staticPath).toFile()
                    : null;
            if (staticDir != null && staticDir.exists()) {
                System.out.println("Static file directory: " + staticDir.getAbsolutePath());
            } else {
                System.out.println("No static file directory configured or found.");
            }
        }

        try {
            // 1. Try dynamic route first
            RouteMatch match = router.findHandler(method, uri);
            if (match != null) {
                return match.getHandler().handle(session, match.getMatcher());
            }

            // 2. If no route, try to serve static content
            // Serve static file if it exists
            if (staticDir != null && staticDir.exists() && staticDir.isDirectory()) {
                File file = resolveStaticFile(uri);
                if (file != null && file.exists() && file.isFile()) {
                    try {
                        String mime = getMimeTypeForFile(file.getName());
                        FileInputStream fis = new FileInputStream(file);
                        return newChunkedResponse(Response.Status.OK, mime, fis);
                    } catch (IOException e) {
                        return ExceptionHandler.handle(e);
                    }
                }
            }

            throw new RouteNotFoundException("No route matched for " + method + " " + uri);
        } catch (Exception e) {
            return ExceptionHandler.handle(e);
        }
    }

    private String getMimeType(String uri) {
        if (uri.endsWith(".html")) return "text/html";
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".json")) return "application/json";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private File resolveStaticFile(String uri) {
        try {
            String decodedPath = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            File requested = new File(staticDir, decodedPath).getCanonicalFile();

            // Prevent path traversal attack
            if (!requested.getPath().startsWith(staticDir.getCanonicalPath())) {
                return null;
            }

            if (requested.isDirectory()) {
                requested = new File(requested, "index.html");
            }

            return requested;
        } catch (IOException e) {
            return null;
        }
    }
}