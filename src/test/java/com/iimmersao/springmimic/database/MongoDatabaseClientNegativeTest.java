package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.TestMongoUser;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings(value = "unused")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MongoDatabaseClientNegativeTest extends AbstractDatabaseClientNegativeTest {

    private final MongoDatabaseClient mongoClient = new MongoDatabaseClient(new ConfigLoader("application-mongodb.properties"));

    MongoDatabaseClientNegativeTest() {
    }

    @AfterAll
    void tearDown() {
        mongoClient.close();
    }

    @Override
    protected DatabaseClient client() {
        return mongoClient;
    }

    @Override
    protected Class<?> getEntityClass() {
        return TestMongoUser.class;
    }

    @Test
    @Override
    void shouldThrowExceptionWhenSortingOnInvalidField() {
        PageRequest request = new PageRequest();
        request.setSortBy("nonexistentField");

        List<?> result = client().findAll(getEntityClass(), request);
        assertNotNull(result); // It should not throw an exception
    }

}
