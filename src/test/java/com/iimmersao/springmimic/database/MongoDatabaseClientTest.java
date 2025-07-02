package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.iimmersao.springmimic.database.MongoDatabaseClient;
import com.iimmersao.springmimic.model.MongoUser;
import org.junit.jupiter.api.*;

        import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MongoDatabaseClientTest {

    private DatabaseClient mongoClient;

    @BeforeAll
    void setup() {
        mongoClient = new MongoDatabaseClient();
    }

    @BeforeEach
    void clean() throws DatabaseException {
        mongoClient.deleteAll(MongoUser.class);
    }

    @Test
    void shouldSaveAndFindUserById() throws DatabaseException {
        MongoUser user = new MongoUser("Alice", "alice@example.com");
        mongoClient.save(user);

        assertNotNull(user.getId());

        Optional<MongoUser> result = mongoClient.findById(MongoUser.class, user.getId());
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getUsername());
    }

    @Test
    void shouldFindAllUsers() throws DatabaseException {
        mongoClient.save(new MongoUser("Bob", "bob@example.com"));
        mongoClient.save(new MongoUser("Charlie", "charlie@example.com"));

        List<MongoUser> users = mongoClient.findAll(MongoUser.class);
        assertEquals(2, users.size());
    }

    @Test
    void shouldUpdateUser() throws DatabaseException {
        MongoUser user = new MongoUser("David", "david@example.com");
        mongoClient.save(user);

        Optional<MongoUser> retrieved = mongoClient.findById(MongoUser.class, user.getId());
        assertTrue(retrieved.isPresent());

        user.setUsername("David Updated");
        mongoClient.updateById(user);

        Optional<MongoUser> updated = mongoClient.findById(MongoUser.class, user.getId());
        assertTrue(updated.isPresent());
        assertEquals("David Updated", updated.get().getUsername());
    }

    @Test
    void shouldUpdateEmail() throws DatabaseException {
        MongoUser user = new MongoUser("David", "david@example.com");
        mongoClient.save(user);

        user.setEmail("david-new@example.com");
        mongoClient.updateById(user);

        Optional<MongoUser> updated = mongoClient.findById(MongoUser.class, user.getId());
        assertTrue(updated.isPresent());
        assertEquals("david-new@example.com", updated.get().getEmail());
    }

    @Test
    void shouldDeleteUserById() throws DatabaseException {
        MongoUser user = new MongoUser("Eve", "eve@example.com");
        mongoClient.save(user);

        mongoClient.deleteById(MongoUser.class, user.getId());
        Optional<MongoUser> result = mongoClient.findById(MongoUser.class, user.getId());
        assertFalse(result.isPresent());
    }

    @Test
    void shouldDeleteAllUsers() throws DatabaseException {
        mongoClient.save(new MongoUser("Fay", "fay@example.com"));
        mongoClient.save(new MongoUser("Greg", "greg@example.com"));

        mongoClient.deleteAll(MongoUser.class);
        List<MongoUser> users = mongoClient.findAll(MongoUser.class);
        assertTrue(users.isEmpty());
    }
}
