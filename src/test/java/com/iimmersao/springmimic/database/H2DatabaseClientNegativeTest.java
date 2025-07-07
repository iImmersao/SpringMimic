package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.H2User;

import java.io.IOException;

class H2DatabaseClientNegativeTest extends AbstractDatabaseClientNegativeTest {

    private final H2DatabaseClient h2Client = new H2DatabaseClient(new ConfigLoader("application-h2.properties"));

    H2DatabaseClientNegativeTest() throws IOException {
    }

    @Override
    protected DatabaseClient client() {
        return h2Client;
    }

    @Override
    protected Class<?> getEntityClass() {
        return H2User.class;
    }
}
