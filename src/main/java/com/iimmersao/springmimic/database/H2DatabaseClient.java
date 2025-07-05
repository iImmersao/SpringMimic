package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Entity;
import com.iimmersao.springmimic.annotations.Table;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.web.PageRequest;

import java.lang.reflect.Field;
import java.sql.*;
        import java.util.*;
        import java.util.stream.Collectors;

public class H2DatabaseClient implements DatabaseClient {

    private final String url;
    private final String username;
    private final String password;

    public H2DatabaseClient(ConfigLoader config) {
        this.url = config.get("h2.url");
        this.username = config.get("h2.username");
        this.password = config.get("h2.password");

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

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize H2 schema", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private String getTableName(Class<?> clazz) {
        //return clazz.getSimpleName().toLowerCase(); // e.g. H2User -> h2user
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
                .collect(Collectors.toList());

        String columns = fields.stream()
                .map(Field::getName)
                .collect(Collectors.joining(", "));

        String placeholders = fields.stream()
                .map(f -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", getTableName(clazz), columns, placeholders);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < fields.size(); i++) {
                Field f = fields.get(i);
                f.setAccessible(true);
                stmt.setObject(i + 1, f.get(entity));
            }

            stmt.executeUpdate();

            // Set generated key back into entity
            Field idField = getIdField(clazz);
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    idField.setAccessible(true);
                    idField.set(entity, generatedKeys.getObject(1));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    @Override
    public <T> Optional<T> findById(Class<T> entityType, Object id) {
        String sql = "SELECT * FROM " + getTableName(entityType) + " WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToObject(entityType, rs));
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to find entity by ID", e);
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityType) {
        String sql = "SELECT * FROM " + getTableName(entityType);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapResultSetToObject(entityType, rs));
            }
            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch all entities", e);
        }
    }

    @Override
    public <T> void updateById(T entity) {
        Class<?> clazz = entity.getClass();
        Field idField = getIdField(clazz);
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !"id".equalsIgnoreCase(f.getName()))
                .collect(Collectors.toList());

        String updates = fields.stream()
                .map(f -> f.getName() + " = ?")
                .collect(Collectors.joining(", "));

        String sql = String.format("UPDATE %s SET %s WHERE id = ?", getTableName(clazz), updates);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setAccessible(true);
                stmt.setObject(i + 1, fields.get(i).get(entity));
            }

            idField.setAccessible(true);
            stmt.setObject(fields.size() + 1, idField.get(entity));

            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Failed to update entity", e);
        }
    }

    @Override
    public <T> void deleteById(Class<T> entityType, Object id) {
        String sql = "DELETE FROM " + getTableName(entityType) + " WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete entity by ID", e);
        }
    }

    @Override
    public <T> void deleteAll(Class<T> entityType) {
        String sql = "DELETE FROM " + getTableName(entityType);

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all entities", e);
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityType, PageRequest pageRequest) {
        String sql = "SELECT * FROM " + getTableName(entityType);

        String whereClause = "";
        List<Object> parameters = new ArrayList<>();

        if (pageRequest.getFilters() != null && !pageRequest.getFilters().isEmpty()) {
            List<String> filters = new ArrayList<>();
            for (Map.Entry<String, String> entry : pageRequest.getFilters().entrySet()) {
                filters.add(entry.getKey() + " = ?");
                parameters.add(entry.getValue());
            }
            whereClause = " WHERE " + String.join(" AND ", filters);
        }

        // Add sorting
        String orderClause = "";
        if (pageRequest.getSortBy() != null) {
            String[] sortParts = pageRequest.getSortBy().split(",");
            String sortField = sortParts[0];

            if (!isValidField(entityType, sortField)) {
                throw new IllegalArgumentException("Invalid sort field: " + sortField);
            }
            String direction = (sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])) ? " DESC" : " ASC";
            orderClause = " ORDER BY " + sortField + direction;
        }

        sql += whereClause + orderClause + " LIMIT ? OFFSET ?";
        parameters.add(pageRequest.getSize());
        parameters.add(pageRequest.getPage() * pageRequest.getSize());

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

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
}
