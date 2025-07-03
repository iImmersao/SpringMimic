package com.iimmersao.springmimic.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.mongodb.client.MongoClients;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

@Bean
public class MySqlDatabaseClient implements DatabaseClient {

    private final Connection connection;
    private ConfigLoader config;

    public MySqlDatabaseClient(ConfigLoader config) {
        this.config = config;

        try {
            String url = config.get("database.url");
            String username = config.get("database.username");
            String password = config.get("database.password");
            connection = DriverManager.getConnection(
                    url, username, password);
        } catch (Exception e) {
            throw new DatabaseException("Failed to connect to MongoDB", e);
        }
    }

    public MySqlDatabaseClient(Connection connection) {
        this.connection = connection;
    }

    @Override
    public <T> Optional<T> findById(Class<T> clazz, Object id) {
        try {
            String table = getTableName(clazz);
            Field idField = getIdField(clazz);
            String column = getColumnName(idField);

            String sql = "SELECT * FROM " + table + " WHERE " + column + " = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setObject(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapResultSetToObject(clazz, rs));
                }
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to find entity by ID", e);
        }

        return Optional.empty();
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        List<T> results = new ArrayList<>();
        try {
            String table = getTableName(clazz);
            String sql = "SELECT * FROM " + table;
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToObject(clazz, rs));
                }
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to find all entities", e);
        }

        return results;
    }

    @Override
    public <T> void save(T entity) {
        Class<?> clazz = entity.getClass();
        Table tableAnnotation = clazz.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new DatabaseException("Missing @Table annotation on class: " + clazz.getName());
        }

        String tableName = tableAnnotation.name();
        Field idField = getIdField(clazz);
        if (idField == null) {
            throw new DatabaseException("No field annotated with @Id in class: " + clazz.getName());
        }

        idField.setAccessible(true);
        Object idValue;
        try {
            idValue = idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Unable to access ID field", e);
        }

        try (Connection conn = getConnection()) {
            if (idValue == null || (idValue instanceof Number && ((Number) idValue).longValue() == 0)) {
                // INSERT
                List<String> columns = new ArrayList<>();
                List<String> placeholders = new ArrayList<>();
                List<Object> values = new ArrayList<>();

                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Id.class)) {
                        continue; // skip ID for auto-generated insert
                    }
                    Column col = field.getAnnotation(Column.class);
                    if (col != null) {
                        columns.add(col.name());
                        placeholders.add("?");
                        values.add(field.get(entity));
                    }
                }

                String sql = "INSERT INTO " + tableName +
                        " (" + String.join(", ", columns) + ") " +
                        "VALUES (" + String.join(", ", placeholders) + ")";
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    setPreparedStatementValues(stmt, values);
                    stmt.executeUpdate();

                    // Retrieve and assign the generated ID
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            Object generatedId = keys.getObject(1);
                            idField.set(entity, convertToFieldType(generatedId, idField.getType()));
                        }
                    }
                }
            } else {
                // UPDATE
                List<String> sets = new ArrayList<>();
                List<Object> values = new ArrayList<>();

                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Id.class)) {
                        continue;
                    }
                    Column col = field.getAnnotation(Column.class);
                    if (col != null) {
                        sets.add(col.name() + " = ?");
                        values.add(field.get(entity));
                    }
                }

                String sql = "UPDATE " + tableName + " SET " +
                        String.join(", ", sets) +
                        " WHERE " + idField.getAnnotation(Column.class).name() + " = ?";
                values.add(idValue);

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    setPreparedStatementValues(stmt, values);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException | IllegalAccessException e) {
            throw new DatabaseException("Failed to save entity", e);
        }
    }

    @Override
    public <T> void updateById(T entity) {
        try {
            Class<?> clazz = entity.getClass();
            String table = getTableName(clazz);

            Field idField = getIdField(clazz);
            idField.setAccessible(true);
            Object idValue = idField.get(entity);
            String idColumn = getColumnName(idField);

            List<String> assignments = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) continue;
                assignments.add(getColumnName(field) + " = ?");
                values.add(field.get(entity));
            }

            String sql = "UPDATE " + table + " SET " + String.join(",", assignments) + " WHERE " + idColumn + " = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) {
                    stmt.setObject(i + 1, values.get(i));
                }
                stmt.setObject(values.size() + 1, idValue);
                stmt.executeUpdate();
            }

        } catch (Exception e) {
            throw new DatabaseException("Failed to update entity", e);
        }
    }

    @Override
    public <T> void deleteById(Class<T> clazz, Object id) {
        try {
            String table = getTableName(clazz);
            Field idField = getIdField(clazz);
            String idColumn = getColumnName(idField);

            String sql = "DELETE FROM " + table + " WHERE " + idColumn + " = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setObject(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete by ID", e);
        }
    }

    @Override
    public <T> void deleteAll(Class<T> clazz) {
        try {
            String table = getTableName(clazz);
            String sql = "DELETE FROM " + table;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete all records", e);
        }
    }

    // ===== Helper Methods =====

    private String getTableName(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new DatabaseException("Missing @Entity annotation on class: " + clazz.getName());
        }
        Table table = clazz.getAnnotation(Table.class);
        return table != null && !table.name().isEmpty() ? table.name() : clazz.getSimpleName().toLowerCase();
    }

    private Field getIdField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new DatabaseException("No @Id field in " + clazz.getName()));
    }

    private String getColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        return column != null && !column.name().isEmpty() ? column.name() : field.getName();
    }

    private <T> T mapResultSetToObject(Class<T> clazz, ResultSet rs) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String colName = getColumnName(field);
            Object value = rs.getObject(colName);
            field.set(instance, value);
        }
        return instance;
    }

    private void setPreparedStatementValues(PreparedStatement stmt, List<Object> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            stmt.setObject(i + 1, values.get(i));
        }
    }

    private Object convertToFieldType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return ((Number) value).intValue();
        } else if (targetType == long.class || targetType == Long.class) {
            return ((Number) value).longValue();
        } else if (targetType == String.class) {
            return value.toString();
        }
        // Add more type conversions if needed
        return value;
    }

    private Connection getConnection() throws SQLException {
        String url = config.get("database.url");
        String username = config.get("database.username");
        String password = config.get("database.password");

        return DriverManager.getConnection(url, username, password);
    }

}
