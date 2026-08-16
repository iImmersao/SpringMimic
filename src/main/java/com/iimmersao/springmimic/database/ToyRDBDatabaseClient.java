package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;

import java.sql.Connection;

@Bean
public class ToyRDBDatabaseClient extends MySqlDatabaseClient {

    public ToyRDBDatabaseClient(ConfigLoader config) {
        this(new TransactionAwareConnectionProvider(new DriverManagerConnectionProvider(
                config.get("database.url"),
                config.get("database.username"),
                config.get("database.password")
        )));
    }

    public ToyRDBDatabaseClient(Connection connection) {
        super(connection, "ToyRDB");
    }

    public ToyRDBDatabaseClient(JdbcConnectionProvider connectionProvider) {
        super(connectionProvider, "ToyRDB");
    }
}
