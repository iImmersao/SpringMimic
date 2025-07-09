package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.H2User;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class H2DatabaseClientTest {

    private static DatabaseClient client;
    private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "password";

    @BeforeAll
    static void setup() throws SQLException, IOException {
        ConfigLoader configLoader = new ConfigLoader("application-h2.properties"); // or your test config file path
        client = new H2DatabaseClient(configLoader);
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255),
                    email VARCHAR(255)
                );
            """);
        }
    }

    @BeforeEach
    void cleanUp() {
        client.deleteAll(H2User.class);
    }

    @Test
    @Order(1)
    void shouldSaveAndFindUserById() {
        H2User user = new H2User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        client.save(user);

        Optional<H2User> found = client.findById(H2User.class, user.getId());
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
    }

    @Test
    @Order(2)
    void shouldUpdateUserById() {
        H2User user = new H2User();
        user.setUsername("bob");
        user.setEmail("bob@example.com");
        client.save(user);
        Integer id = user.getId();

        user.setUsername("bobby");
        client.updateById(user);

        Optional<H2User> updated = client.findById(H2User.class, id);
        assertTrue(updated.isPresent());
        assertEquals("bobby", updated.get().getUsername());
    }

    @Test
    @Order(3)
    void shouldDeleteUserById() {
        H2User user = new H2User();
        user.setUsername("carol");
        user.setUsername("carol@example.com");
        client.save(user);

        client.deleteById(H2User.class, user.getId());

        Optional<H2User> deleted = client.findById(H2User.class, user.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    @Order(4)
    void shouldReturnAllUsers() {
        H2User user1 = new H2User();
        user1.setUsername("user1");
        user1.setEmail("u1@example.com");
        client.save(user1);

        H2User user2 = new H2User();
        user2.setUsername("user2");
        user2.setEmail("u2@example.com");
        client.save(user2);

        List<H2User> users = client.findAll(H2User.class);
        assertEquals(2, users.size());
    }

    @Test
    @Order(5)
    void shouldPaginateResults() {
        for (int i = 0; i < 15; i++) {
            H2User user = new H2User();
            user.setUsername("user" + i);
            user.setEmail("u" + i + "@example.com");
            client.save(user);
        }

        PageRequest pageRequest = new PageRequest();
        // page 1, size 10
        pageRequest.setPage(1);
        pageRequest.setSize(10);
        List<H2User> page = client.findAll(H2User.class, pageRequest);
        assertEquals(5, page.size());
    }

    @Test
    @Order(6)
    void shouldFilterResults() {
        H2User dave = new H2User();
        dave.setUsername("dave");
        dave.setEmail("dave@example.com");
        H2User danny = new H2User();
        danny.setUsername("danny");
        danny.setEmail("danny@example.com");
        client.save(dave);
        client.save(danny);

        Map<String, Object> filters = new HashMap<>();
        filters.put("username", "danny");
        PageRequest request = new PageRequest();
        request.setPage(0);
        request.setSize(10);
        request.setFilters(filters);

        List<H2User> filtered = client.findAll(H2User.class, request);
        assertEquals(1, filtered.size());
        assertEquals("danny", filtered.getFirst().getUsername());
    }

    @Test
    @Order(7)
    void shouldSortResultsDescending() {
        H2User user1 = new H2User();
        user1.setUsername("eve");
        user1.setEmail("eve@example.com");
        H2User user2 = new H2User();
        user2.setUsername("alice");
        user2.setEmail("alice@example.com");
        client.save(user1);
        client.save(user2);

        PageRequest request = new PageRequest();
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("username,desc");
        List<H2User> results = client.findAll(H2User.class, request);

        assertEquals("eve", results.get(0).getUsername());
        assertEquals("alice", results.get(1).getUsername());
    }

    @Test
    void shouldDetectUserExistsInH2() {
        H2User user = new H2User();
        user.setUsername("h2user");
        user.setEmail("h2@example.com");
        client.save(user);

        assertTrue(client.existsBy(H2User.class, "username", "h2user"));
    }

    @Test
    void shouldReturnFalseIfUserNotFoundInH2() {
        assertFalse(client.existsBy(H2User.class, "username", "ghost"));
    }

    @Test
    void shouldCountUsersInH2ByEmail() {
        H2User user1 = new H2User();
        user1.setUsername("x");
        user1.setEmail("h2@example.com");
        client.save(user1);
        H2User user2 = new H2User();
        user2.setUsername("y");
        user2.setEmail("h2@example.com");
        client.save(user2);
        H2User user3 = new H2User();
        user3.setUsername("z");
        user3.setEmail("other@example.com");
        client.save(user3);

        assertEquals(2L, client.countBy(H2User.class, "email", "h2@example.com"));
    }
}
