package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.dialect.ToyRdbDialect;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.SingleConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;
import com.iimmersao.springmimic.database.schema.ToyRdbSchemaManager;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Bean
public class ToyRDBDatabaseClient extends JdbcDatabaseClient {

    public ToyRDBDatabaseClient(ConfigLoader config) {
        this(config, scanConfiguredEntities(config));
    }

    public ToyRDBDatabaseClient(ConfigLoader config, Set<Class<?>> entityClasses) {
        this(createConnectionProvider(config));
        manageSchema(config, entityClasses);
    }

    public ToyRDBDatabaseClient(Connection connection) {
        this(new SingleConnectionProvider(connection));
    }

    public ToyRDBDatabaseClient(JdbcConnectionProvider connectionProvider) {
        super(connectionProvider, new ToyRdbDialect());
    }

    private static JdbcConnectionProvider createConnectionProvider(ConfigLoader config) {
        validateConfiguration(config);
        DriverCheck.loadDriver(config, "toyrdb");
        return new TransactionAwareConnectionProvider(new DriverManagerConnectionProvider(
                config.get("database.url"),
                config.get("database.username"),
                config.get("database.password")
        ));
    }

    private static void validateConfiguration(ConfigLoader config) {
        String dialect = configuredDialect(config);
        if (!isToyRdbDialect(dialect)) {
            throw new IllegalArgumentException("Unsupported ToyRDB database.dialect: " + dialect);
        }

        String ddlAuto = config.get("database.ddl-auto", "none").trim().toLowerCase(Locale.ROOT);
        if (!Set.of("none", "validate", "create", "update").contains(ddlAuto)) {
            throw new IllegalArgumentException("Unsupported ToyRDB database.ddl-auto: " + ddlAuto);
        }
    }

    private static String configuredDialect(ConfigLoader config) {
        String dialect = config.get("database.dialect", "");
        if (dialect == null || dialect.isBlank()) {
            dialect = config.get("database.platform", "toyrdb");
        }
        return dialect.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isToyRdbDialect(String dialect) {
        return "toyrdb".equals(dialect)
                || "com.iimmersao.toyrdb.hibernate.toyrdbdialect".equals(dialect);
    }

    private static Set<Class<?>> scanConfiguredEntities(ConfigLoader config) {
        String packages = config.get("database.schema.packages", "");
        if (packages == null || packages.isBlank()) {
            return Set.of();
        }
        String[] basePackages = Arrays.stream(packages.split(","))
                .map(String::trim)
                .filter(packageName -> !packageName.isEmpty())
                .toArray(String[]::new);
        if (basePackages.length == 0) {
            return Set.of();
        }
        return new EntityScanner().scanEntities(basePackages);
    }

    private void manageSchema(ConfigLoader config, Set<Class<?>> entityClasses) {
        new ToyRdbSchemaManager(getConnectionProvider()).apply(config.get("database.ddl-auto", "none"), entityClasses);
    }
}
