package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.*;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.SingleConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.web.PageRequest;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Bean
public class MySqlDatabaseClient implements DatabaseClient {

    private final JdbcConnectionProvider connectionProvider;

    public MySqlDatabaseClient(ConfigLoader config) {
        this(new TransactionAwareConnectionProvider(new DriverManagerConnectionProvider(
                config.get("database.url"),
                config.get("database.username"),
                config.get("database.password")
        )));
    }

    public MySqlDatabaseClient(Connection connection) {
        this(new SingleConnectionProvider(connection));
    }

    public MySqlDatabaseClient(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public <T> Optional<T> findById(Class<T> clazz, Object id) {
        Connection conn = null;
        try {
            String table = getTableName(clazz);
            Field idField = getIdField(clazz);
            String column = getColumnName(idField);
            String sql = "SELECT * FROM " + table + " WHERE " + column + " = ?";
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToObject(clazz, rs));
                    }
                }
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to find entity by ID", e);
        } finally {
            releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        Connection conn = null;
        try {
            String table = getTableName(clazz);
            String sql = "SELECT * FROM " + table;
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToObject(clazz, rs));
                }
                return results;
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to find all entities", e);
        } finally {
            releaseConnection(conn);
        }
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
        idField.setAccessible(true);

        Object idValue;
        try {
            idValue = idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Unable to access ID field", e);
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (idValue == null || (idValue instanceof Number && ((Number) idValue).longValue() == 0)) {
                insert(entity, clazz, tableName, idField, conn);
            } else {
                update(entity, clazz, tableName, idField, idValue, conn);
            }
        } catch (SQLException | IllegalAccessException e) {
            throw new DatabaseException("Failed to save entity", e);
        } finally {
            releaseConnection(conn);
        }
    }

    private <T> void insert(T entity, Class<?> clazz, String tableName, Field idField, Connection conn)
            throws SQLException, IllegalAccessException {
        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
                continue;
            }
            Column col = field.getAnnotation(Column.class);
            if (col != null) {
                columns.add(col.name());
                placeholders.add("?");
                values.add(field.get(entity));
            }
        }

        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES (" +
                String.join(", ", placeholders) + ")";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setPreparedStatementValues(stmt, values);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    Object generatedId = keys.getObject(1);
                    idField.set(entity, convertToFieldType(generatedId, idField.getType()));
                }
            }
        }
    }

    private <T> void update(T entity, Class<?> clazz, String tableName, Field idField, Object idValue, Connection conn)
            throws SQLException, IllegalAccessException {
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

        String sql = "UPDATE " + tableName + " SET " + String.join(", ", sets) +
                " WHERE " + idField.getAnnotation(Column.class).name() + " = ?";
        values.add(idValue);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setPreparedStatementValues(stmt, values);
            stmt.executeUpdate();
        }
    }

    @Override
    public <T> void updateById(T entity) {
        Connection conn = null;
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
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) {
                    stmt.setObject(i + 1, values.get(i));
                }
                stmt.setObject(values.size() + 1, idValue);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to update entity", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public <T> void deleteById(Class<T> clazz, Object id) {
        Connection conn = null;
        try {
            Field idField = getIdField(clazz);
            Class<?> expectedType = idField.getType();
            if (!expectedType.isInstance(id)) {
                throw new IllegalArgumentException("Invalid ID type: expected " + expectedType.getSimpleName());
            }
            String table = getTableName(clazz);
            String idColumn = getColumnName(idField);
            String sql = "DELETE FROM " + table + " WHERE " + idColumn + " = ?";
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete by ID", e);
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public <T> void deleteAll(Class<T> clazz) {
        Connection conn = null;
        try {
            String table = getTableName(clazz);
            String sql = "DELETE FROM " + table;
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete all records", e);
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

    private Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    private void releaseConnection(Connection connection) {
        try {
            connectionProvider.releaseConnection(connection);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to release MySQL connection", e);
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
        if (targetType.isAssignableFrom(value.getClass())) return value;
        if (targetType == int.class || targetType == Integer.class) return ((Number) value).intValue();
        if (targetType == long.class || targetType == Long.class) return ((Number) value).longValue();
        if (targetType == String.class) return value.toString();
        return value;
    }

    private void validateFieldNames(Class<?> entityType, Map<String, Object> filters) {
        Set<String> validFields = Arrays.stream(entityType.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
        for (String field : filters.keySet()) {
            if (!validFields.contains(field)) {
                throw new IllegalArgumentException("Unknown field in filter: " + field);
            }
        }
    }
}
