package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.MongoUser;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MongoDatabaseClientNegativeTest extends AbstractDatabaseClientNegativeTest {

    private final MongoDatabaseClient mongoClient = new MongoDatabaseClient(new ConfigLoader("application-mongodb.properties"));

    MongoDatabaseClientNegativeTest() throws IOException {
    }

    @Override
    protected DatabaseClient client() {
        return mongoClient;
    }

    @Override
    protected Class<?> getEntityClass() {
        return MongoUser.class;
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
