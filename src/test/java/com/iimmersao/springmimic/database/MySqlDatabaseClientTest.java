package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.annotations.*;

import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MySqlDatabaseClientTest {

    private DatabaseClient dbClient;

    private ConfigLoader configLoader;

    @Entity
    @Table(name = "users")
    public static class User {
        @Id
        @Column(name = "id")
        public Integer id;

        @Column(name = "username")
        public String username;

        @Column(name = "email")
        public String email;

        public User() {}

        public User(String username) {
            this.username = username;
        }
    }

    @BeforeAll
    void init() throws Exception {
        configLoader = new ConfigLoader("application.properties"); // or your test config file path
        /*
        String url = "jdbc:mysql://localhost:3306/sakila";
        String user = "root";
        String pass = "Basement99!";

        // Drop and recreate table for clean testing
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DROP TABLE IF EXISTS users");
            stmt.executeUpdate("""
                CREATE TABLE users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255)
                )
            """);
        }
         */

        dbClient = new MySqlDatabaseClient(configLoader);
    }

    @BeforeEach
    void clearTable() {
        dbClient.deleteAll(User.class);
    }

    @Test
    void shouldSaveAndFindById() {
        User user = new User("alice");
        dbClient.save(user);

        assertNotNull(user.id);

        Optional<User> retrieved = dbClient.findById(User.class, user.id);
        assertTrue(retrieved.isPresent());
        assertEquals("alice", retrieved.get().username);
    }

    @Test
    void shouldFindAllUsers() {
        dbClient.save(new User("bob"));
        dbClient.save(new User("carol"));

        List<User> users = dbClient.findAll(User.class);
        assertEquals(2, users.size());
    }

    @Test
    void shouldUpdateUser() {
        User user = new User("dave");
        dbClient.save(user);

        user.username = "david";
        dbClient.updateById(user);

        Optional<User> updated = dbClient.findById(User.class, user.id);
        assertTrue(updated.isPresent());
        assertEquals("david", updated.get().username);
    }

    @Test
    void shouldDeleteUserById() {
        User user = new User("erin");
        dbClient.save(user);

        dbClient.deleteById(User.class, user.id);

        Optional<User> deleted = dbClient.findById(User.class, user.id);
        assertTrue(deleted.isEmpty());
    }

    @Test
    void shouldDeleteAllUsers() {
        dbClient.save(new User("frank"));
        dbClient.save(new User("grace"));

        dbClient.deleteAll(User.class);

        List<User> users = dbClient.findAll(User.class);
        assertTrue(users.isEmpty());
    }

    @Test
    void shouldReturnPagedResults() throws Exception {
        // Arrange
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.username = "user" + i;
            user.email = "user" + i + "@example.com";
            dbClient.save(user);
        }

        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(1);  // second page
        pageRequest.setSize(3);  // 3 per page

        // Act
        List<?> results = dbClient.findAll(User.class, pageRequest);

        // Assert
        assertEquals(3, results.size(), "Expected 3 users on page 1");
    }

    @Test
    void shouldReturnSortedResults() throws Exception {
        // Arrange
        String[] usernames = { "zeta", "alpha", "beta" };
        for (String name : usernames) {
            User user = new User();
            user.username = name;
            user.email = name + "@example.com";
            dbClient.save(user);
        }

        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortBy("username, asc");

        // Act
        List<?> results = dbClient.findAll(User.class, pageRequest);
        List<String> resultUsernames = results.stream()
                .map(u -> ((User) u).username)
                .collect(Collectors.toList());

        // Assert
        assertEquals(List.of("alpha", "beta", "zeta"), resultUsernames);
    }

    @Test
    void shouldReturnFilteredResults() throws Exception {
        // Arrange
        User targetUser = new User();
        targetUser.username = "filterme";
        targetUser.email = "filterme@example.com";
        dbClient.save(targetUser);

        User otherUser = new User();
        otherUser.username = "other";
        otherUser.email = "other@example.com";
        dbClient.save(otherUser);

        PageRequest pageRequest = new PageRequest();
        Map<String, String> filters = new HashMap<>();
        filters.put("username", "filterme");
        pageRequest.setFilters(filters);

        // Act
        List<?> results = dbClient.findAll(User.class, pageRequest);

        // Assert
        assertEquals(1, results.size());
        assertEquals("filterme", ((User) results.get(0)).username);
    }
}
