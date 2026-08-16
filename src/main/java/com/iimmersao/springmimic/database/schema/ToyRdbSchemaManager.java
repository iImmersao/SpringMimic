package com.iimmersao.springmimic.database.schema;

import com.iimmersao.springmimic.annotations.Column;
import com.iimmersao.springmimic.annotations.Entity;
import com.iimmersao.springmimic.annotations.GeneratedValue;
import com.iimmersao.springmimic.annotations.Id;
import com.iimmersao.springmimic.annotations.Table;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.exceptions.DatabaseException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ToyRdbSchemaManager {

    private final JdbcConnectionProvider connectionProvider;

    public ToyRdbSchemaManager(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void apply(String ddlAuto, Set<Class<?>> entityClasses) {
        String mode = normalizeMode(ddlAuto);
        if ("none".equals(mode)) {
            return;
        }
        if ("create-drop".equals(mode)) {
            throw new IllegalArgumentException("Unsupported ToyRDB database.ddl-auto: create-drop");
        }
        if (!Set.of("validate", "create", "update").contains(mode)) {
            throw new IllegalArgumentException("Unsupported ToyRDB database.ddl-auto: " + mode);
        }
        if (entityClasses == null || entityClasses.isEmpty()) {
            throw new IllegalArgumentException("ToyRDB database.ddl-auto=" + mode + " requires entity classes");
        }

        switch (mode) {
            case "validate" -> validate(entityClasses);
            case "create" -> create(entityClasses);
            case "update" -> update(entityClasses);
            default -> throw new IllegalStateException("Unexpected ToyRDB schema mode: " + mode);
        }
    }

    private void validate(Set<Class<?>> entityClasses) {
        Connection connection = null;
        try {
            connection = connectionProvider.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            for (Class<?> entityClass : entityClasses) {
                validateEntity(metaData, entityClass);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to validate ToyRDB schema", e);
        } finally {
            releaseConnection(connection);
        }
    }

    private void create(Set<Class<?>> entityClasses) {
        Connection connection = null;
        try {
            connection = connectionProvider.getConnection();
            try (Statement statement = connection.createStatement()) {
                for (Class<?> entityClass : entityClasses) {
                    statement.executeUpdate("DROP TABLE IF EXISTS " + tableName(entityClass));
                    statement.executeUpdate(createTableSql(entityClass));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create ToyRDB schema", e);
        } finally {
            releaseConnection(connection);
        }
    }

    private void update(Set<Class<?>> entityClasses) {
        Connection connection = null;
        try {
            connection = connectionProvider.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            try (Statement statement = connection.createStatement()) {
                for (Class<?> entityClass : entityClasses) {
                    String tableName = tableName(entityClass);
                    if (!tableExists(metaData, tableName)) {
                        statement.executeUpdate(createTableSql(entityClass));
                        continue;
                    }
                    for (Field field : persistentFields(entityClass)) {
                        String columnName = columnName(field);
                        if (!columnExists(metaData, tableName, columnName)) {
                            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                                    + columnDefinition(field, false));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update ToyRDB schema", e);
        } finally {
            releaseConnection(connection);
        }
    }

    private void validateEntity(DatabaseMetaData metaData, Class<?> entityClass) throws SQLException {
        String tableName = tableName(entityClass);
        if (!tableExists(metaData, tableName)) {
            throw new DatabaseException("ToyRDB schema validation failed: missing table " + tableName);
        }
        for (Field field : persistentFields(entityClass)) {
            String columnName = columnName(field);
            if (!columnExists(metaData, tableName, columnName)) {
                throw new DatabaseException("ToyRDB schema validation failed: missing column "
                        + tableName + "." + columnName);
            }
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private String createTableSql(Class<?> entityClass) {
        String columns = persistentFields(entityClass).stream()
                .map(field -> columnDefinition(field, true))
                .collect(Collectors.joining(", "));
        return "CREATE TABLE " + tableName(entityClass) + " (" + columns + ")";
    }

    private String columnDefinition(Field field, boolean includePrimaryKey) {
        StringBuilder sql = new StringBuilder(columnName(field)).append(" ").append(sqlType(field.getType()));
        if (includePrimaryKey && field.isAnnotationPresent(Id.class)) {
            if (field.isAnnotationPresent(GeneratedValue.class) && isIntegerType(field.getType())) {
                sql.append(" AUTO_INCREMENT");
            }
            sql.append(" PRIMARY KEY");
        }
        return sql.toString();
    }

    private ArrayList<Field> persistentFields(Class<?> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String tableName(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new DatabaseException("Missing @Entity annotation on class: " + entityClass.getName());
        }
        Table table = entityClass.getAnnotation(Table.class);
        return table != null && !table.name().isEmpty()
                ? table.name()
                : entityClass.getSimpleName().toLowerCase(Locale.ROOT);
    }

    private String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        return column != null && !column.name().isEmpty() ? column.name() : field.getName();
    }

    private String sqlType(Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return "INT";
        }
        if (type == long.class || type == Long.class) {
            return "BIGINT";
        }
        if (type == short.class || type == Short.class) {
            return "SMALLINT";
        }
        if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
            return "FLOAT";
        }
        if (type == BigDecimal.class) {
            return "DECIMAL(19,2)";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "BOOLEAN";
        }
        if (type == LocalDate.class || type == java.sql.Date.class) {
            return "DATE";
        }
        if (type == LocalTime.class || type == java.sql.Time.class) {
            return "TIME";
        }
        if (type == LocalDateTime.class || type == java.sql.Timestamp.class) {
            return "TIMESTAMP";
        }
        if (type == String.class || type == char.class || type == Character.class) {
            return "TEXT";
        }
        throw new IllegalArgumentException("Unsupported ToyRDB field type: " + type.getName());
    }

    private boolean isIntegerType(Class<?> type) {
        return type == int.class || type == Integer.class || type == long.class || type == Long.class;
    }

    private String normalizeMode(String ddlAuto) {
        if (ddlAuto == null || ddlAuto.isBlank()) {
            return "none";
        }
        return ddlAuto.trim().toLowerCase(Locale.ROOT);
    }

    private void releaseConnection(Connection connection) {
        try {
            connectionProvider.releaseConnection(connection);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to release ToyRDB schema connection", e);
        }
    }
}
