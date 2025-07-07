package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.web.PageRequest;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

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
            throw new DatabaseException("Failed to connect to MySql", e);
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
            Field idField = getIdField(clazz); // assume this uses reflection
            Class<?> expectedType = idField.getType();
            if (!expectedType.isInstance(id)) {
                throw new IllegalArgumentException("Invalid ID type: expected " + expectedType.getSimpleName());
            }

            String table = getTableName(clazz);
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

    @Override
    public <T> List<T> findAll(Class<T> entityType, PageRequest pageRequest) {
        try {
            validateFieldNames(entityType, pageRequest.getFilters());
            // continue with building query and execution
        } catch (IllegalArgumentException ex) {
            // Optional: log and return empty list instead of failing
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch paginated results", e);
        }

        String tableName = getTableName(entityType);
        List<String> whereClauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        // Build WHERE clause with optional LIKE handling
        for (Map.Entry<String, Object> entry : pageRequest.getFilters().entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (pageRequest.getLikeFields().contains(field)) {
                whereClauses.add(field + " LIKE ?");
                parameters.add("%" + value + "%");
            } else {
                whereClauses.add(field + " = ?");
                parameters.add(value);
            }
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }

        // Add sorting
        // Add ORDER BY clause
        String orderClause = "";
        if (pageRequest.getSortBy() != null) {
            String[] sortParts = pageRequest.getSortBy().split(",");
            String sortField = sortParts[0];

            if (!isValidField(entityType, sortField)) {
                throw new IllegalArgumentException("Invalid sort field: " + sortField);
            }
            String direction = (sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])) ? " DESC" : " ASC";
            orderClause = " ORDER BY " + sortField + direction;
            sql.append(orderClause);
        }

        // Add pagination (LIMIT + OFFSET)
        sql.append(" LIMIT ? OFFSET ?");
        parameters.add(pageRequest.getSize());
        parameters.add(pageRequest.getPage() * pageRequest.getSize());

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapResultSetToObject(entityType, rs));
            }
            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch paginated results", e);
        }
    }

    @Override
    public boolean existsBy(Class<?> entityType, String fieldName, Object value) {
        String table = getTableName(entityType);
        String sql = "SELECT 1 FROM " + table + " WHERE " + fieldName + " = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute existsBy", e);
        }
    }

    @Override
    public long countBy(Class<?> entityType, String fieldName, Object value) {
        String table = getTableName(entityType);
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + fieldName + " = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute countBy", e);
        }
    }

    // ===== Helper Methods =====

    private boolean isValidField(Class<?> clazz, String fieldName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

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

    private void validateFieldNames(Class<?> entityType, Map<String, Object> filters) {
        Set<String> validFields = Arrays.stream(entityType.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        for (String field : filters.keySet()) {
            if (!validFields.contains(field)) {
                throw new IllegalArgumentException("Unknown field in filter: " + field);
            }
        }
    }
}
