package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.model.TestMongoUser;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MongoDatabaseClientTest {

    private DatabaseClient mongoClient;

    private ConfigLoader configLoader;

    @BeforeAll
    void setup() throws IOException {
        configLoader = new ConfigLoader("application-mongodb.properties"); // or your test config file path
        mongoClient = new MongoDatabaseClient(configLoader);
    }

    @BeforeEach
    void clean() throws DatabaseException {
        mongoClient.deleteAll(TestMongoUser.class);
    }

    @Test
    void shouldSaveAndFindUserById() throws DatabaseException {
        TestMongoUser user = new TestMongoUser("Alice", "alice@example.com");
        mongoClient.save(user);

        assertNotNull(user.getId());

        Optional<TestMongoUser> result = mongoClient.findById(TestMongoUser.class, user.getId());
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getUsername());
    }

    @Test
    void shouldFindAllUsers() throws DatabaseException {
        mongoClient.save(new TestMongoUser("Bob", "bob@example.com"));
        mongoClient.save(new TestMongoUser("Charlie", "charlie@example.com"));

        List<TestMongoUser> users = mongoClient.findAll(TestMongoUser.class);
        assertEquals(2, users.size());
    }

    @Test
    void shouldUpdateUser() throws DatabaseException {
        TestMongoUser user = new TestMongoUser("David", "david@example.com");
        mongoClient.save(user);

        Optional<TestMongoUser> retrieved = mongoClient.findById(TestMongoUser.class, user.getId());
        assertTrue(retrieved.isPresent());

        user.setUsername("David Updated");
        mongoClient.updateById(user);

        Optional<TestMongoUser> updated = mongoClient.findById(TestMongoUser.class, user.getId());
        assertTrue(updated.isPresent());
        assertEquals("David Updated", updated.get().getUsername());
    }

    @Test
    void shouldUpdateEmail() throws DatabaseException {
        TestMongoUser user = new TestMongoUser("David", "david@example.com");
        mongoClient.save(user);

        user.setEmail("david-new@example.com");
        mongoClient.updateById(user);

        Optional<TestMongoUser> updated = mongoClient.findById(TestMongoUser.class, user.getId());
        assertTrue(updated.isPresent());
        assertEquals("david-new@example.com", updated.get().getEmail());
    }

    @Test
    void shouldDeleteUserById() throws DatabaseException {
        TestMongoUser user = new TestMongoUser("Eve", "eve@example.com");
        mongoClient.save(user);

        mongoClient.deleteById(TestMongoUser.class, user.getId());
        Optional<TestMongoUser> result = mongoClient.findById(TestMongoUser.class, user.getId());
        assertFalse(result.isPresent());
    }

    @Test
    void shouldDeleteAllUsers() throws DatabaseException {
        mongoClient.save(new TestMongoUser("Fay", "fay@example.com"));
        mongoClient.save(new TestMongoUser("Greg", "greg@example.com"));

        mongoClient.deleteAll(TestMongoUser.class);
        List<TestMongoUser> users = mongoClient.findAll(TestMongoUser.class);
        assertTrue(users.isEmpty());
    }

    @Test
    void shouldReturnPagedResults() throws Exception {
        // Arrange
        for (int i = 1; i <= 10; i++) {
            TestMongoUser user = new TestMongoUser();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            mongoClient.save(user);
        }

        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(1);  // second page
        pageRequest.setSize(3);  // 3 per page

        // Act
        List<?> results = mongoClient.findAll(TestMongoUser.class, pageRequest);

        // Assert
        assertEquals(3, results.size(), "Expected 3 users on page 1");
    }

    @Test
    void shouldReturnSortedResults() throws Exception {
        // Arrange
        String[] usernames = { "zeta", "alpha", "beta" };
        for (String name : usernames) {
            TestMongoUser user = new TestMongoUser();
            user.setUsername(name);
            user.setEmail(name + "@example.com");
            mongoClient.save(user);
        }

        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortBy("username, asc");

        // Act
        List<?> results = mongoClient.findAll(TestMongoUser.class, pageRequest);
        List<String> resultUsernames = results.stream()
                .map(u -> ((TestMongoUser) u).getUsername())
                .collect(Collectors.toList());

        // Assert
        assertEquals(List.of("alpha", "beta", "zeta"), resultUsernames);
    }

    @Test
    void shouldReturnFilteredResults() throws Exception {
        // Arrange
        TestMongoUser targetUser = new TestMongoUser();
        targetUser.setUsername("filterme");
        targetUser.setEmail("filterme@example.com");
        mongoClient.save(targetUser);

        TestMongoUser otherUser = new TestMongoUser();
        otherUser.setUsername("other");
        otherUser.setEmail("other@example.com");
        mongoClient.save(otherUser);

        PageRequest pageRequest = new PageRequest();
        Map<String, String> filters = new HashMap<>();
        filters.put("username", "filterme");
        pageRequest.setFilters(filters);

        // Act
        List<?> results = mongoClient.findAll(TestMongoUser.class, pageRequest);

        // Assert
        assertEquals(1, results.size());
        assertEquals("filterme", ((TestMongoUser) results.get(0)).getUsername());
    }
}
