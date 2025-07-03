package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.model.TestMongoUser;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
}
