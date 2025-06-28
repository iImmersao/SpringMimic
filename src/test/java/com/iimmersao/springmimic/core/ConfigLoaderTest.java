package com.iimmersao.springmimic.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    void shouldReturnExistingProperty() {
        String value = ConfigLoader.get("test.key", "default");
        assertEquals("hello", value);
    }

    @Test
    void shouldReturnNullForMissingProperty() {
        String value = ConfigLoader.get("missing.key", "fallback");
        assertEquals("fallback", value);
    }

    @Test
    void shouldReturnParsedIntValue() {
        int port = ConfigLoader.getInt("test.port", 0);
        assertEquals(1234, port);
    }

    @Test
    void shouldReturnDefaultIfMissing() {
        int fallback = ConfigLoader.getInt("not.found", 42);
        assertEquals(42, fallback);
    }

    @Test
    void shouldReturnDefaultIfNotInteger() {
        int fallback = ConfigLoader.getInt("test.keynum", 99); // "hello" is not an int
        assertEquals(99, fallback);
    }
}
