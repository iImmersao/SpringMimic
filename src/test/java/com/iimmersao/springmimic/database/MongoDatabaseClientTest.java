package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.model.TestMongoUser;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(value = "unused")
public class MongoDatabaseClientTest {

    private DatabaseClient mongoClient;

    @BeforeAll
    void setup() {
        ConfigLoader configLoader = new ConfigLoader("application-mongodb.properties"); // or your test config file path
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
    void shouldGracefullyNotFindUserById() throws DatabaseException {
        assertThrows(IllegalArgumentException.class, () -> mongoClient.findById(TestMongoUser.class, "123"));
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
    void shouldReturnPagedResults() {
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
    void shouldReturnSortedResults() {
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
    void shouldReturnFilteredResults() {
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
        Map<String, Object> filters = new HashMap<>();
        filters.put("username", "filterme");
        pageRequest.setFilters(filters);

        // Act
        List<?> results = mongoClient.findAll(TestMongoUser.class, pageRequest);

        // Assert
        assertEquals(1, results.size());
        assertEquals("filterme", ((TestMongoUser) results.getFirst()).getUsername());
    }

    @Test
    void shouldFindUsersByEmailContains() {
        // Arrange
        TestMongoUser user1 = new TestMongoUser();
        user1.setUsername("david");
        user1.setEmail("david.smith@example.com");
        TestMongoUser user2 = new TestMongoUser();
        user2.setUsername("emma");
        user2.setEmail("emma.jones@example.com");
        TestMongoUser user3 = new TestMongoUser();
        user3.setUsername("frank");
        user3.setEmail("frankie@another.com");

        mongoClient.save(user1);
        mongoClient.save(user2);
        mongoClient.save(user3);

        PageRequest request = new PageRequest();
        Map<String, Object> filters = new HashMap<>();
        filters.put("email", "example.com");
        request.setFilters(filters);
        request.addLikeField("email");

        // Act
        List<TestMongoUser> result = mongoClient.findAll(TestMongoUser.class, request);

        // Assert
        assertEquals(2, result.size());
        List<String> emails = result.stream().map(TestMongoUser::getEmail).toList();
        assertTrue(emails.contains("david.smith@example.com"));
        assertTrue(emails.contains("emma.jones@example.com"));
    }

    @Test
    void shouldReturnTrueWhenUserExistsInMongo() {
        TestMongoUser user = new TestMongoUser();
        user.setUsername("mongoUser");
        user.setEmail("mongo@example.com");
        mongoClient.save(user);

        boolean exists = mongoClient.existsBy(TestMongoUser.class, "username", "mongoUser");
        assertTrue(exists);
    }

    @Test
    void shouldReturnFalseWhenUserMissingInMongo() {
        boolean exists = mongoClient.existsBy(TestMongoUser.class, "username", "not_found");
        assertFalse(exists);
    }

    @Test
    void shouldCountUsersInMongoByEmail() {
        TestMongoUser user1 = new TestMongoUser("a", "multi@example.com");
        mongoClient.save(user1);
        TestMongoUser user2 = new TestMongoUser("b", "multi@example.com");
        mongoClient.save(user2);
        TestMongoUser user3 = new TestMongoUser("c", "solo@example.com");
        mongoClient.save(user3);

        long count = mongoClient.countBy(TestMongoUser.class, "email", "multi@example.com");
        assertEquals(2L, count);
    }
}
