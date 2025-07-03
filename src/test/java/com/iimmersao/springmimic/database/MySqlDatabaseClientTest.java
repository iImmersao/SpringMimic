package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;
import com.iimmersao.springmimic.annotations.*;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

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
}
