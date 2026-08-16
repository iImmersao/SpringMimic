package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;

import java.util.Locale;
import java.util.Map;

public class DriverCheck {

    private static final String DRIVER_CLASS_PROPERTY = "database.driver-class-name";
    private static final Map<String, String> DEFAULT_DRIVER_CLASSES = Map.of(
            "h2", "org.h2.Driver",
            "mysql", "com.mysql.cj.jdbc.Driver",
            "toyrdb", "com.iimmersao.toyrdb.jdbc.ToyRDBDriver"
    );

    private DriverCheck() {
    }

    public static void loadDriver(ConfigLoader config, String dbType) {
        loadDriver(dbType, resolveDriverClassName(config, dbType));
    }

    public static void loadDriver(String dbType, String driverClassName) {
        if (driverClassName == null || driverClassName.isBlank()) {
            return;
        }

        try {
            Class.forName(driverClassName.trim());
        } catch (ClassNotFoundException e) {
            throw new DatabaseException(
                    "JDBC driver class not found for db.type=" + normalizeDbType(dbType) + ": " + driverClassName,
                    e
            );
        }
    }

    public static String resolveDriverClassName(ConfigLoader config, String dbType) {
        String defaultDriverClassName = defaultDriverClassName(dbType);
        String configuredDriverClassName = config.get(DRIVER_CLASS_PROPERTY, defaultDriverClassName);
        if (configuredDriverClassName == null || configuredDriverClassName.isBlank()) {
            return defaultDriverClassName;
        }
        return configuredDriverClassName.trim();
    }

    public static String defaultDriverClassName(String dbType) {
        return DEFAULT_DRIVER_CLASSES.get(normalizeDbType(dbType));
    }

    private static String normalizeDbType(String dbType) {
        return dbType == null ? "" : dbType.trim().toLowerCase(Locale.ROOT);
    }

    public static void main(String[] args) {
        String dbType = args.length > 0 ? args[0] : "mysql";
        String driverClassName = args.length > 1 ? args[1] : defaultDriverClassName(dbType);
        loadDriver(dbType, driverClassName);
        System.out.println("Driver loaded successfully: " + driverClassName);
    }
}
