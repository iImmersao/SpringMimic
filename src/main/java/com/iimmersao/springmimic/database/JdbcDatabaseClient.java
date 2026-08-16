package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Column;
import com.iimmersao.springmimic.annotations.Entity;
import com.iimmersao.springmimic.annotations.Id;
import com.iimmersao.springmimic.annotations.Table;
import com.iimmersao.springmimic.database.dialect.SqlDialect;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.web.PageRequest;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JdbcDatabaseClient implements DatabaseClient {

    private final JdbcConnectionProvider connectionProvider;
    private final SqlDialect dialect;

    public JdbcDatabaseClient(JdbcConnectionProvider connectionProvider, SqlDialect dialect) {
        this.connectionProvider = connectionProvider;
        this.dialect = dialect;
    }

    @Override
    public <T> Optional<T> findById(Class<T> clazz, Object id) {
        if (dialect.invalidFindByIdReturnsEmpty() && !isValidId(clazz, id)) {
            return Optional.empty();
        }

        Connection conn = null;
        try {
            Field idField = getIdField(clazz);
            String sql = "SELECT * FROM " + getTableName(clazz) + " WHERE " + getColumnName(idField) + " = ?";
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
        return executeEntityQuery(clazz, "SELECT * FROM " + getTableName(clazz), List.of(), "Failed to find all entities");
    }

    @Override
    public <T> void save(T entity) {
        Class<?> clazz = entity.getClass();
        String tableName = getTableName(clazz);
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
            if (idValue == null || (idValue instanceof Number number && number.longValue() == 0)) {
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

    @Override
    public <T> void updateById(T entity) {
        Connection conn = null;
        try {
            Class<?> clazz = entity.getClass();
            String table = getTableName(clazz);
            Field idField = getIdField(clazz);
            idField.setAccessible(true);
            Object idValue = idField.get(entity);
            List<String> assignments = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                assignments.add(getColumnName(field) + " = ?");
                values.add(field.get(entity));
            }

            String sql = "UPDATE " + table + " SET " + String.join(",", assignments)
                    + " WHERE " + getColumnName(idField) + " = ?";
            values.add(idValue);
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPreparedStatementValues(stmt, values);
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
            Class<?> expectedType = boxedType(idField.getType());
            if (id != null && !expectedType.isInstance(id)) {
                throw new IllegalArgumentException("Invalid ID type: expected " + expectedType.getSimpleName());
            }
            String sql = "DELETE FROM " + getTableName(clazz) + " WHERE " + getColumnName(idField) + " = ?";
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
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + getTableName(clazz))) {
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
            validateFieldNames(entityType, pageRequest.getFilters().keySet());
            validateFieldNames(entityType, pageRequest.getLikeFields());
        } catch (IllegalArgumentException ex) {
            return Collections.emptyList();
        }

        if (requiresInMemoryFiltering(pageRequest)) {
            return findAllWithInMemoryFiltering(entityType, pageRequest);
        }

        String tableName = getTableName(entityType);
        List<String> whereClauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : pageRequest.getFilters().entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            if (pageRequest.getLikeFields().contains(field)) {
                whereClauses.add(getColumnName(entityType, field) + " LIKE ?");
                parameters.add("%" + value + "%");
            } else {
                whereClauses.add(getColumnName(entityType, field) + " = ?");
                parameters.add(value);
            }
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        appendWhere(sql, whereClauses);
        appendOrderBy(sql, entityType, pageRequest.getSortBy());
        if (dialect.supportsLimitOffset()) {
            sql.append(dialect.limitOffsetClause());
            parameters.add(pageRequest.getSize());
            parameters.add(pageRequest.getPage() * pageRequest.getSize());
        }

        List<T> results = executeEntityQuery(entityType, sql.toString(), parameters, "Failed to fetch paginated results");
        if (!dialect.supportsLimitOffset()) {
            return page(results, pageRequest);
        }
        return results;
    }

    @Override
    public boolean existsBy(Class<?> entityType, String fieldName, Object value) {
        Field field = getField(entityType, fieldName);
        String columnName = getColumnName(field);
        String sql = "SELECT " + dialect.existsSelectExpression(columnName) + " FROM "
                + getTableName(entityType) + " WHERE " + columnName + " = ?";
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
        Field field = getField(entityType, fieldName);
        String sql = "SELECT COUNT(*) FROM " + getTableName(entityType)
                + " WHERE " + getColumnName(field) + " = ?";
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
            columns.add(getColumnName(field));
            placeholders.add("?");
            values.add(field.get(entity));
        }

        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES ("
                + String.join(", ", placeholders) + ")";
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
            sets.add(getColumnName(field) + " = ?");
            values.add(field.get(entity));
        }

        String sql = "UPDATE " + tableName + " SET " + String.join(", ", sets)
                + " WHERE " + getColumnName(idField) + " = ?";
        values.add(idValue);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setPreparedStatementValues(stmt, values);
            stmt.executeUpdate();
        }
    }

    private boolean requiresInMemoryFiltering(PageRequest pageRequest) {
        return !dialect.supportsLike() && !pageRequest.getLikeFields().isEmpty();
    }

    private <T> List<T> findAllWithInMemoryFiltering(Class<T> entityType, PageRequest pageRequest) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(getTableName(entityType));
        appendOrderBy(sql, entityType, pageRequest.getSortBy());

        return page(executeEntityQuery(entityType, sql.toString(), List.of(), "Failed to fetch paginated results")
                .stream()
                .filter(entity -> matchesFilters(entity, pageRequest))
                .toList(), pageRequest);
    }

    private void appendWhere(StringBuilder sql, List<String> whereClauses) {
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }
    }

    private void appendOrderBy(StringBuilder sql, Class<?> entityType, String sortBy) {
        if (sortBy == null) {
            return;
        }

        String[] sortParts = sortBy.split(",");
        String sortField = sortParts[0].trim();
        if (!isValidField(entityType, sortField)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortField);
        }

        String direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1].trim()) ? " DESC" : " ASC";
        sql.append(" ORDER BY ").append(getColumnName(entityType, sortField)).append(direction);
    }

    private <T> List<T> executeEntityQuery(Class<T> entityType, String sql, List<Object> parameters, String errorMessage) {
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPreparedStatementValues(stmt, parameters);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapResultSetToObject(entityType, rs));
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(errorMessage, e);
        } finally {
            releaseConnection(conn);
        }
    }

    private <T> List<T> page(List<T> results, PageRequest pageRequest) {
        int fromIndex = Math.min(pageRequest.getPage() * pageRequest.getSize(), results.size());
        int toIndex = Math.min(fromIndex + pageRequest.getSize(), results.size());
        return new ArrayList<>(results.subList(fromIndex, toIndex));
    }

    private boolean matchesFilters(Object entity, PageRequest pageRequest) {
        try {
            for (Map.Entry<String, Object> entry : pageRequest.getFilters().entrySet()) {
                Field field = getField(entity.getClass(), entry.getKey());
                field.setAccessible(true);
                Object fieldValue = field.get(entity);
                Object filterValue = entry.getValue();
                if (pageRequest.getLikeFields().contains(entry.getKey())) {
                    if (!contains(fieldValue, filterValue)) {
                        return false;
                    }
                } else if (!equalsValue(fieldValue, filterValue)) {
                    return false;
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Failed to inspect entity fields", e);
        }
    }

    private boolean contains(Object fieldValue, Object filterValue) {
        if (fieldValue == null || filterValue == null) {
            return false;
        }
        return fieldValue.toString().contains(filterValue.toString().replace("%", ""));
    }

    private boolean equalsValue(Object fieldValue, Object filterValue) {
        if (fieldValue == null) {
            return filterValue == null;
        }
        return fieldValue.equals(filterValue);
    }

    private Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    private void releaseConnection(Connection connection) {
        try {
            connectionProvider.releaseConnection(connection);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to release " + dialect.name() + " connection", e);
        }
    }

    private String getTableName(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new DatabaseException("Missing @Entity annotation on class: " + clazz.getName());
        }
        Table table = clazz.getAnnotation(Table.class);
        return table != null && !table.name().isEmpty() ? table.name() : clazz.getSimpleName().toLowerCase(Locale.ROOT);
    }

    private Field getIdField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new DatabaseException("No @Id field in " + clazz.getName()));
    }

    private String getColumnName(Class<?> entityType, String fieldName) {
        return getColumnName(getField(entityType, fieldName));
    }

    private String getColumnName(Field field) {
        if (!dialect.useColumnAnnotations()) {
            return field.getName();
        }
        Column column = field.getAnnotation(Column.class);
        return column != null && !column.name().isEmpty() ? column.name() : field.getName();
    }

    private Field getField(Class<?> entityType, String fieldName) {
        return Arrays.stream(entityType.getDeclaredFields())
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such field: " + fieldName));
    }

    private boolean isValidField(Class<?> clazz, String fieldName) {
        return Arrays.stream(clazz.getDeclaredFields()).anyMatch(field -> field.getName().equals(fieldName));
    }

    private <T> T mapResultSetToObject(Class<T> clazz, ResultSet rs) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            field.set(instance, rs.getObject(getColumnName(field)));
        }
        return instance;
    }

    private void setPreparedStatementValues(PreparedStatement stmt, List<Object> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            stmt.setObject(i + 1, values.get(i));
        }
    }

    private Object convertToFieldType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        Class<?> boxedTargetType = boxedType(targetType);
        if (boxedTargetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (boxedTargetType == Integer.class) {
            return ((Number) value).intValue();
        }
        if (boxedTargetType == Long.class) {
            return ((Number) value).longValue();
        }
        if (boxedTargetType == String.class) {
            return value.toString();
        }
        return value;
    }

    private boolean isValidId(Class<?> entityType, Object id) {
        if (id == null) {
            return true;
        }
        Class<?> expectedType = boxedType(getIdField(entityType).getType());
        if (expectedType.isInstance(id)) {
            return true;
        }
        if (expectedType == Integer.class && id instanceof String text) {
            try {
                Integer.parseInt(text);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private Class<?> boxedType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private void validateFieldNames(Class<?> entityType, Set<String> fieldNames) {
        Set<String> validFields = Arrays.stream(entityType.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        for (String fieldName : fieldNames) {
            if (!validFields.contains(fieldName)) {
                throw new IllegalArgumentException("Unknown field in filter: " + fieldName);
            }
        }
    }
}
