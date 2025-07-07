package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.MySqlUser;

import java.io.IOException;

class MySqlDatabaseClientNegativeTest extends AbstractDatabaseClientNegativeTest {

    private final MySqlDatabaseClient mysqlClient = new MySqlDatabaseClient(new ConfigLoader("application.properties"));

    MySqlDatabaseClientNegativeTest() throws IOException {
    }

    @Override
    protected DatabaseClient client() {
        return mysqlClient;
    }

    @Override
    protected Class<?> getEntityClass() {
        return MySqlUser.class;
    }
}
