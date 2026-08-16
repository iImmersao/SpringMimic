package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Column;
import com.iimmersao.springmimic.annotations.Entity;
import com.iimmersao.springmimic.annotations.GeneratedValue;
import com.iimmersao.springmimic.annotations.Id;
import com.iimmersao.springmimic.annotations.Table;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToyRDBDatabaseClientTest {

    @TempDir
    private Path tempDir;

    private DatabaseClient client;
    private String jdbcUrl;

    @BeforeEach
    void setUp() throws SQLException {
        Path databaseFile = tempDir.resolve("springmimic-toyrdb.data");
        jdbcUrl = "jdbc:toyrdb:" + databaseFile.toAbsolutePath();
        ConfigLoader config = config(Map.of(
                "database.url", jdbcUrl,
                "database.username", "",
                "database.password", ""
        ));
        client = new ToyRDBDatabaseClient(config);
        createSchema(jdbcUrl);
    }

    @Test
    void shouldSaveAndFindById() {
        User user = new User("alice", "alice@example.com");

        client.save(user);

        assertNotNull(user.id);
        Optional<User> found = client.findById(User.class, user.id);
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().username);
        assertEquals("alice@example.com", found.get().email);
    }

    @Test
    void shouldFindAllUsers() {
        client.save(new User("bob", "bob@example.com"));
        client.save(new User("carol", "carol@example.com"));

        List<User> users = client.findAll(User.class);

        assertEquals(2, users.size());
    }

    @Test
    void shouldUpdateUserById() {
        User user = new User("dave", "dave@example.com");
        client.save(user);

        user.username = "david";
        client.updateById(user);

        Optional<User> updated = client.findById(User.class, user.id);
        assertTrue(updated.isPresent());
        assertEquals("david", updated.get().username);
    }

    @Test
    void shouldDeleteUserById() {
        User user = new User("erin", "erin@example.com");
        client.save(user);

        client.deleteById(User.class, user.id);

        assertTrue(client.findById(User.class, user.id).isEmpty());
    }

    @Test
    void shouldDeleteAllUsers() {
        client.save(new User("frank", "frank@example.com"));
        client.save(new User("grace", "grace@example.com"));

        client.deleteAll(User.class);

        assertTrue(client.findAll(User.class).isEmpty());
    }

    @Test
    void shouldReturnPagedResults() {
        for (int i = 1; i <= 10; i++) {
            client.save(new User("user" + i, "user" + i + "@example.com"));
        }

        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(1);
        pageRequest.setSize(3);

        List<User> results = client.findAll(User.class, pageRequest);

        assertEquals(3, results.size());
    }

    @Test
    void shouldReturnSortedResults() {
        client.save(new User("zeta", "zeta@example.com"));
        client.save(new User("alpha", "alpha@example.com"));
        client.save(new User("beta", "beta@example.com"));

        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortBy("username, asc");

        List<User> results = client.findAll(User.class, pageRequest);

        assertEquals(List.of("alpha", "beta", "zeta"), results.stream().map(user -> user.username).toList());
    }

    @Test
    void shouldReturnFilteredResults() {
        client.save(new User("filterme", "filterme@example.com"));
        client.save(new User("other", "other@example.com"));

        PageRequest pageRequest = new PageRequest();
        Map<String, Object> filters = new HashMap<>();
        filters.put("username", "filterme");
        pageRequest.setFilters(filters);

        List<User> results = client.findAll(User.class, pageRequest);

        assertEquals(1, results.size());
        assertEquals("filterme", results.getFirst().username);
    }

    @Test
    void shouldFindUsersByUsernameContains() {
        client.save(new User("alice_smith", "alice@example.com"));
        client.save(new User("bob_jones", "bob@example.com"));
        client.save(new User("charlie", "charlie@example.com"));

        PageRequest request = new PageRequest();
        Map<String, Object> filters = new HashMap<>();
        filters.put("username", "bob");
        request.setFilters(filters);
        request.addLikeField("username");

        List<User> results = client.findAll(User.class, request);

        assertEquals(1, results.size());
        assertEquals("bob_jones", results.getFirst().username);
    }

    @Test
    void shouldMapNumericGeneratedIdToStringEntityId() throws SQLException {
        String schemaUrl = jdbcUrl("string-id.data");
        createSchema(schemaUrl);
        DatabaseClient stringIdClient = new ToyRDBDatabaseClient(config(Map.of(
                "database.url", schemaUrl,
                "database.username", "",
                "database.password", ""
        )));
        StringIdUser user = new StringIdUser("string-id", "string-id@example.com");

        stringIdClient.save(user);

        assertEquals("1", user.id);
        Optional<StringIdUser> found = stringIdClient.findById(StringIdUser.class, user.id);
        assertTrue(found.isPresent());
        assertEquals("1", found.get().id);
        assertEquals("string-id", found.get().username);
        assertEquals(List.of("1"), stringIdClient.findAll(StringIdUser.class).stream().map(u -> u.id).toList());
    }

    @Test
    void shouldReturnTrueWhenUserExistsByUsername() {
        client.save(new User("jdoe", "jdoe@example.com"));

        assertTrue(client.existsBy(User.class, "username", "jdoe"));
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExistByUsername() {
        assertFalse(client.existsBy(User.class, "username", "ghost"));
    }

    @Test
    void shouldCountUsersByEmail() {
        client.save(new User("a", "shared@example.com"));
        client.save(new User("b", "shared@example.com"));
        client.save(new User("c", "other@example.com"));

        assertEquals(2L, client.countBy(User.class, "email", "shared@example.com"));
    }

    @Test
    void shouldCreateSchemaWhenDdlAutoIsCreate() {
        String schemaUrl = jdbcUrl("schema-create.data");
        DatabaseClient schemaClient = new ToyRDBDatabaseClient(config(Map.of(
                "database.url", schemaUrl,
                "database.username", "",
                "database.password", "",
                "database.ddl-auto", "create"
        )), Set.of(User.class));

        User user = new User("created", "created@example.com");
        schemaClient.save(user);

        assertNotNull(user.id);
        assertTrue(schemaClient.findById(User.class, user.id).isPresent());
    }

    @Test
    void shouldValidateSchemaWhenDdlAutoIsValidate() throws SQLException {
        String schemaUrl = jdbcUrl("schema-validate.data");
        createSchema(schemaUrl);

        assertNotNull(new ToyRDBDatabaseClient(config(Map.of(
                "database.url", schemaUrl,
                "database.username", "",
                "database.password", "",
                "database.ddl-auto", "validate"
        )), Set.of(User.class)));
    }

    @Test
    void shouldFailValidateWhenTableIsMissing() {
        String schemaUrl = jdbcUrl("schema-validate-missing.data");

        DatabaseException exception = assertThrows(
                DatabaseException.class,
                () -> new ToyRDBDatabaseClient(config(Map.of(
                        "database.url", schemaUrl,
                        "database.username", "",
                        "database.password", "",
                        "database.ddl-auto", "validate"
                )), Set.of(User.class))
        );
        assertEquals("ToyRDB schema validation failed: missing table users", exception.getMessage());
    }

    @Test
    void shouldUpdateSchemaWhenDdlAutoIsUpdate() throws SQLException {
        String schemaUrl = jdbcUrl("schema-update.data");
        createPartialSchema(schemaUrl);
        DatabaseClient schemaClient = new ToyRDBDatabaseClient(config(Map.of(
                "database.url", schemaUrl,
                "database.username", "",
                "database.password", "",
                "database.ddl-auto", "update"
        )), Set.of(User.class));

        User user = new User("updated", "updated@example.com");
        schemaClient.save(user);

        Optional<User> found = schemaClient.findById(User.class, user.id);
        assertTrue(found.isPresent());
        assertEquals("updated@example.com", found.get().email);
    }

    @Test
    void shouldRejectUnsupportedDdlAutoMode() {
        ConfigLoader config = config(Map.of(
                "database.url", jdbcUrl,
                "database.username", "",
                "database.password", "",
                "database.ddl-auto", "create-drop"
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ToyRDBDatabaseClient(config, Set.of(User.class))
        );
        assertEquals("Unsupported ToyRDB database.ddl-auto: create-drop", exception.getMessage());
    }

    private String jdbcUrl(String fileName) {
        return "jdbc:toyrdb:" + tempDir.resolve(fileName).toAbsolutePath();
    }

    private void createSchema(String schemaUrl) throws SQLException {
        DriverCheck.loadDriver(config(Map.of()), "toyrdb");
        try (Connection connection = DriverManager.getConnection(schemaUrl, "", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE users (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      username TEXT,
                      email TEXT
                    )
                    """);
        }
    }

    private void createPartialSchema(String schemaUrl) throws SQLException {
        DriverCheck.loadDriver(config(Map.of()), "toyrdb");
        try (Connection connection = DriverManager.getConnection(schemaUrl, "", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE users (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      username TEXT
                    )
                    """);
        }
    }

    private static ConfigLoader config(Map<String, String> values) {
        return new ConfigLoader() {
            @Override
            public String get(String key, String defaultValue) {
                return values.getOrDefault(key, defaultValue);
            }

            @Override
            public String get(String key) {
                return values.get(key);
            }
        };
    }

    @Entity
    @Table(name = "users")
    @SuppressWarnings("unused")
    public static class User {
        @Id
        @GeneratedValue
        @Column(name = "id")
        public Integer id;

        @Column(name = "username")
        public String username;

        @Column(name = "email")
        public String email;

        public User() {
        }

        public User(String username, String email) {
            this.username = username;
            this.email = email;
        }
    }

    @Entity
    @Table(name = "users")
    @SuppressWarnings("unused")
    public static class StringIdUser {
        @Id
        @GeneratedValue
        @Column(name = "id")
        public String id;

        @Column(name = "username")
        public String username;

        @Column(name = "email")
        public String email;

        public StringIdUser() {
        }

        public StringIdUser(String username, String email) {
            this.username = username;
            this.email = email;
        }
    }
}
