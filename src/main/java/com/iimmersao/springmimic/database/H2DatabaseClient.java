package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.annotations.Entity;
import com.iimmersao.springmimic.annotations.Table;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.web.PageRequest;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Bean
public class H2DatabaseClient implements DatabaseClient {

    private final JdbcConnectionProvider connectionProvider;

    public H2DatabaseClient(ConfigLoader config) {
        this(config, new TransactionAwareConnectionProvider(new DriverManagerConnectionProvider(
                config.get("h2.url"),
                config.get("h2.username"),
                config.get("h2.password")
        )));
    }

    public H2DatabaseClient(ConfigLoader config, JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        try {
            Class.forName("org.h2.Driver");
            initializeSchema();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 Driver not found", e);
        }
    }

    private void initializeSchema() {
        String sql = """
        CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(100) NOT NULL,
            email VARCHAR(255),
            active BOOLEAN,
            age INT
        );
    """;

        Connection conn = null;
        try {
            conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize H2 schema", e);
        } finally {
            releaseConnection(conn);
        }
    }

    private Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    private void releaseConnection(Connection connection) {
        try {
            connectionProvider.releaseConnection(connection);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to release H2 connection", e);
        }
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
                .filter(f -> f.getName().equalsIgnoreCase("id"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No ID field found"));
    }

    @Override
    public <T> void save(T entity) {
        Class<?> clazz = entity.getClass();
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !"id".equalsIgnoreCase(f.getName()))
                .toList();

        String columns = fields.stream().map(Field::getName).collect(Collectors.joining(", "));
        String placeholders = fields.stream().map(f -> "?").collect(Collectors.joining(", "));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", getTableName(clazz), columns, placeholders);

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    f.setAccessible(true);
                    stmt.setObject(i + 1, f.get(entity));
                }
                stmt.executeUpdate();

                Field idField = getIdField(clazz);
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Object key = generatedKeys.getObject(1);
                        idField.setAccessible(true);
                        idField.set(entity, key);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save entity", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public <T> Optional<T> findById(Class<T> entityType, Object id) {
        if (!isValidId(id)) {
            return Optional.empty();
        }

        String sql = "SELECT * FROM " + getTableName(entityType) + " WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToObject(entityType, rs));
                    }
                    return Optional.empty();
                }
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to find entity by ID", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityType) {
        String sql = "SELECT * FROM " + getTableName(entityType);
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToObject(entityType, rs));
                }
                return results;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch all entities", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public <T> void updateById(T entity) {
        Class<?> clazz = entity.getClass();
        Field idField = getIdField(clazz);
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !"id".equalsIgnoreCase(f.getName()))
                .toList();

        String updates = fields.stream().map(f -> f.getName() + " = ?").collect(Collectors.joining(", "));
        String sql = String.format("UPDATE %s SET %s WHERE id = ?", getTableName(clazz), updates);
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < fields.size(); i++) {
                    fields.get(i).setAccessible(true);
                    stmt.setObject(i + 1, fields.get(i).get(entity));
                }
                idField.setAccessible(true);
                stmt.setObject(fields.size() + 1, idField.get(entity));
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update entity", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public <T> void deleteById(Class<T> entityType, Object id) {
        String sql = "DELETE FROM " + getTableName(entityType) + " WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete entity by ID", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public <T> void deleteAll(Class<T> entityType) {
        String sql = "DELETE FROM " + getTableName(entityType);
        Connection conn = null;
        try {
            conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all entities", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityType, PageRequest pageRequest) {
        try {
            validateFieldNames(entityType, pageRequest.getFilters());
        } catch (IllegalArgumentException ex) {
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch paginated results", e);
        }

        String tableName = getTableName(entityType);
        List<String> whereClauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

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
        if (pageRequest.getSortBy() != null) {
            String[] sortParts = pageRequest.getSortBy().split(",");
            String sortField = sortParts[0].trim();
            if (!isValidField(entityType, sortField)) {
                throw new IllegalArgumentException("Invalid sort field: " + sortField);
            }
            String direction = (sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1].trim())) ? " DESC" : " ASC";
            sql.append(" ORDER BY ").append(sortField).append(direction);
        }

        sql.append(" LIMIT ? OFFSET ?");
        parameters.add(pageRequest.getSize());
        parameters.add(pageRequest.getPage() * pageRequest.getSize());

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapResultSetToObject(entityType, rs));
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch paginated results", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public boolean existsBy(Class<?> entityType, String fieldName, Object value) {
        String table = getTableName(entityType);
        String sql = "SELECT 1 FROM " + table + " WHERE " + fieldName + " = ? LIMIT 1";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, value);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute existsBy", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public long countBy(Class<?> entityType, String fieldName, Object value) {
        String table = getTableName(entityType);
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + fieldName + " = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, value);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute countBy", e);
        } finally {
            releaseConnection(conn);
        }
    }

    private boolean isValidField(Class<?> clazz, String fieldName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private <T> T mapResultSetToObject(Class<T> clazz, ResultSet rs) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = rs.getObject(field.getName());
            field.set(instance, value);
        }
        return instance;
    }

    private void validateFieldNames(Class<?> entityType, Map<String, Object> filters) {
        Set<String> validFields = Arrays.stream(entityType.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
        for (String field : filters.keySet()) {
            if (!validFields.contains(field)) {
                throw new IllegalArgumentException("Unknown field in filter: " + field);
            }
        }
    }

    private boolean isValidId(Object id) {
        if (id instanceof Integer) {
            return true;
        } else if (id instanceof String) {
            try {
                Integer.parseInt((String) id);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
