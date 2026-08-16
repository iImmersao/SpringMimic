package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DriverCheckTest {

    @Test
    void resolvesDefaultToyRdbDriverClassName() {
        assertEquals(
                "com.iimmersao.toyrdb.jdbc.ToyRDBDriver",
                DriverCheck.resolveDriverClassName(config(Map.of()), "toyrdb")
        );
    }

    @Test
    void loadsDefaultToyRdbDriverClassName() {
        assertDoesNotThrow(() -> DriverCheck.loadDriver(config(Map.of()), "toyrdb"));
    }

    @Test
    void configuredDriverClassNameOverridesDefault() {
        ConfigLoader config = config(Map.of("database.driver-class-name", "org.h2.Driver"));

        assertEquals("org.h2.Driver", DriverCheck.resolveDriverClassName(config, "toyrdb"));
        assertDoesNotThrow(() -> DriverCheck.loadDriver(config, "toyrdb"));
    }

    @Test
    void missingConfiguredDriverClassNameThrowsDatabaseException() {
        ConfigLoader config = config(Map.of("database.driver-class-name", "example.MissingDriver"));

        DatabaseException exception = assertThrows(
                DatabaseException.class,
                () -> DriverCheck.loadDriver(config, "toyrdb")
        );
        assertEquals(
                "JDBC driver class not found for db.type=toyrdb: example.MissingDriver",
                exception.getMessage()
        );
    }

    private static ConfigLoader config(Map<String, String> values) {
        return new ConfigLoader() {
            @Override
            public String get(String key, String defaultValue) {
                return values.getOrDefault(key, defaultValue);
            }

            @Override
            public String get(String key) {
                return values.get(key);
            }
        };
    }
}
