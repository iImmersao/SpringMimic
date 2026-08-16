package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.dialect.MySqlDialect;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.SingleConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;

import java.sql.Connection;

@Bean
public class MySqlDatabaseClient extends JdbcDatabaseClient {

    public MySqlDatabaseClient(ConfigLoader config) {
        this(new TransactionAwareConnectionProvider(new DriverManagerConnectionProvider(
                config.get("database.url"),
                config.get("database.username"),
                config.get("database.password")
        )));
    }

    public MySqlDatabaseClient(Connection connection) {
        this(new SingleConnectionProvider(connection));
    }

    public MySqlDatabaseClient(JdbcConnectionProvider connectionProvider) {
        super(connectionProvider, new MySqlDialect());
    }
}
