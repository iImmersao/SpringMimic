package com.iimmersao.springmimic;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.ToyRDBDatabaseClient;
import com.iimmersao.springmimic.transaction.JdbcTransactionManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpringMimicApplicationRunnerToyRDBTest {

    @Test
    void createsToyRdbDatabaseSetup() {
        SpringMimicApplicationRunner.DatabaseSetup setup =
                SpringMimicApplicationRunner.createDatabaseSetup(config(Map.of(
                        "db.type", "toyrdb",
                        "database.url", "jdbc:toyrdb:target/toyrdb-setup.data",
                        "database.username", "",
                        "database.password", ""
                )));

        assertInstanceOf(ToyRDBDatabaseClient.class, setup.databaseClient());
        assertInstanceOf(JdbcTransactionManager.class, setup.transactionManager());
    }

    @Test
    void rejectsUnsupportedToyRdbDdlAutoMode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SpringMimicApplicationRunner.createDatabaseSetup(config(Map.of(
                        "db.type", "toyrdb",
                        "database.url", "jdbc:h2:mem:toyrdb-ddl-auto",
                        "database.username", "sa",
                        "database.password", "",
                        "database.driver-class-name", "org.h2.Driver",
                        "database.ddl-auto", "create-drop"
                )))
        );
    }

    @Test
    void rejectsUnsupportedToyRdbDialect() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SpringMimicApplicationRunner.createDatabaseSetup(config(Map.of(
                        "db.type", "toyrdb",
                        "database.url", "jdbc:h2:mem:toyrdb-dialect",
                        "database.username", "sa",
                        "database.password", "",
                        "database.driver-class-name", "org.h2.Driver",
                        "database.dialect", "mysql"
                )))
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

            @Override
            public int getInt(String key, int defaultValue) {
                String value = values.get(key);
                if (value == null) {
                    return defaultValue;
                }
                return Integer.parseInt(value);
            }
        };
    }
}
