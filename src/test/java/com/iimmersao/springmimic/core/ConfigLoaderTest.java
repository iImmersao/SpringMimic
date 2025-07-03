package com.iimmersao.springmimic.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    private ConfigLoader configLoader;

    @BeforeEach
    public void setUp() throws IOException {
        configLoader = new ConfigLoader("application.properties"); // or your test config file path
    }

    @Test
    void shouldReturnExistingProperty() {
        String value = configLoader.get("test.key", "default");
        assertEquals("hello", value);
    }

    @Test
    void shouldReturnNullForMissingProperty() {
        String value = configLoader.get("missing.key", "fallback");
        assertEquals("fallback", value);
    }

    @Test
    void shouldReturnParsedIntValue() {
        int port = configLoader.getInt("test.port", 0);
        assertEquals(1234, port);
    }

    @Test
    void shouldReturnDefaultIfMissing() {
        int fallback = configLoader.getInt("not.found", 42);
        assertEquals(42, fallback);
    }

    @Test
    void shouldReturnDefaultIfNotInteger() {
        int fallback = configLoader.getInt("test.keynum", 99); // "hello" is not an int
        assertEquals(99, fallback);
    }

    @Test
    public void shouldStripInlineCommentsAndTrimValues() {
        String dbType = configLoader.get("db.type", "");
        assertNotNull(dbType);
        assertFalse(dbType.contains("#"), "Config value should not contain inline comments");
        assertEquals("mysql", dbType, "Config value should be trimmed and comments removed");
    }
}
