package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.SingleConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.web.PageRequest;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Bean
public class ToyRDBDatabaseClient extends MySqlDatabaseClient {

    private final JdbcConnectionProvider connectionProvider;

    public ToyRDBDatabaseClient(ConfigLoader config) {
        this(createConnectionProvider(config));
    }

    public ToyRDBDatabaseClient(Connection connection) {
        this(new SingleConnectionProvider(connection));
    }

    public ToyRDBDatabaseClient(JdbcConnectionProvider connectionProvider) {
        super(connectionProvider, "ToyRDB");
        this.connectionProvider = connectionProvider;
    }

    @Override
    public <T> List<T> findAll(Class<T> entityType, PageRequest pageRequest) {
        try {
            validateFieldNames(entityType, pageRequest.getFilters());
            validateFieldNames(entityType, pageRequest.getLikeFields());
        } catch (IllegalArgumentException ex) {
            return Collections.emptyList();
        }

        if (!pageRequest.getLikeFields().isEmpty()) {
            return findAllWithJavaFiltering(entityType, pageRequest);
        }

        String tableName = getTableName(entityType);
        List<String> whereClauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : pageRequest.getFilters().entrySet()) {
            whereClauses.add(getColumnName(entityType, entry.getKey()) + " = ?");
            parameters.add(entry.getValue());
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }
        appendOrderBy(sql, entityType, pageRequest.getSortBy());

        List<T> results = executeQuery(entityType, sql.toString(), parameters);
        return page(results, pageRequest);
    }

    @Override
    public boolean existsBy(Class<?> entityType, String fieldName, Object value) {
        if (!isValidField(entityType, fieldName)) {
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }

        String columnName = getColumnName(entityType, fieldName);
        String sql = "SELECT " + columnName + " FROM " + getTableName(entityType) + " WHERE " + columnName + " = ?";
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
        String dialect = config.get("database.dialect", "toyrdb").trim().toLowerCase(Locale.ROOT);
        if (!"toyrdb".equals(dialect)) {
            throw new IllegalArgumentException("Unsupported ToyRDB database.dialect: " + dialect);
        }

        String ddlAuto = config.get("database.ddl-auto", "none").trim().toLowerCase(Locale.ROOT);
        if (!"none".equals(ddlAuto)) {
            throw new IllegalArgumentException("Unsupported ToyRDB database.ddl-auto: " + ddlAuto);
        }
    }

    private <T> List<T> findAllWithJavaFiltering(Class<T> entityType, PageRequest pageRequest) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(getTableName(entityType));
        appendOrderBy(sql, entityType, pageRequest.getSortBy());

        List<T> results = executeQuery(entityType, sql.toString(), List.of());
        results = results.stream()
                .filter(entity -> matchesFilters(entity, pageRequest))
                .toList();
        return page(results, pageRequest);
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

    private <T> List<T> executeQuery(Class<T> entityType, String sql, List<Object> parameters) {
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        return fieldValue.toString().contains(filterValue.toString());
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
            throw new DatabaseException("Failed to release ToyRDB connection", e);
        }
    }

    private String getTableName(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(com.iimmersao.springmimic.annotations.Entity.class)) {
            throw new DatabaseException("Missing @Entity annotation on class: " + clazz.getName());
        }
        com.iimmersao.springmimic.annotations.Table table =
                clazz.getAnnotation(com.iimmersao.springmimic.annotations.Table.class);
        return table != null && !table.name().isEmpty() ? table.name() : clazz.getSimpleName().toLowerCase(Locale.ROOT);
    }

    private String getColumnName(Class<?> entityType, String fieldName) {
        return getColumnName(getField(entityType, fieldName));
    }

    private String getColumnName(Field field) {
        com.iimmersao.springmimic.annotations.Column column =
                field.getAnnotation(com.iimmersao.springmimic.annotations.Column.class);
        return column != null && !column.name().isEmpty() ? column.name() : field.getName();
    }

    private Field getField(Class<?> entityType, String fieldName) {
        return Arrays.stream(entityType.getDeclaredFields())
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown field: " + fieldName));
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

    private void validateFieldNames(Class<?> entityType, Map<String, Object> filters) {
        validateFieldNames(entityType, filters.keySet());
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
