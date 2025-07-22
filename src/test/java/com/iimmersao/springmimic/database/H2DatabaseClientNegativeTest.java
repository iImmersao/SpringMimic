package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.H2User;

@SuppressWarnings(value = "unused")
class H2DatabaseClientNegativeTest extends AbstractDatabaseClientNegativeTest {

    private final H2DatabaseClient h2Client = new H2DatabaseClient(new ConfigLoader("h2"));

    H2DatabaseClientNegativeTest() {
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
