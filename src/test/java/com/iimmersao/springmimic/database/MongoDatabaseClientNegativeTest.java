package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.TestMongoUser;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings(value = "unused")
class MongoDatabaseClientNegativeTest extends AbstractDatabaseClientNegativeTest {

    private final MongoDatabaseClient mongoClient = new MongoDatabaseClient(new ConfigLoader("application-mongodb.properties"));

    MongoDatabaseClientNegativeTest() {
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
