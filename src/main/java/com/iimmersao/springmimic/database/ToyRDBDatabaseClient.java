package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.dialect.ToyRdbDialect;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.SingleConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;

import java.sql.Connection;
import java.util.Locale;

@Bean
public class ToyRDBDatabaseClient extends JdbcDatabaseClient {

    public ToyRDBDatabaseClient(ConfigLoader config) {
        this(createConnectionProvider(config));
    }

    public ToyRDBDatabaseClient(Connection connection) {
        this(new SingleConnectionProvider(connection));
    }

    public ToyRDBDatabaseClient(JdbcConnectionProvider connectionProvider) {
        super(connectionProvider, new ToyRdbDialect());
    }

    private static JdbcConnectionProvider createConnectionProvider(ConfigLoader config) {
        validateConfiguration(config);
        DriverCheck.loadDriver(config, "toyrdb");
        return new TransactionAwareConnectionProvider(new DriverManagerConnectionProvider(
                config.get("database.url"),
                config.get("database.username"),
                config.get("database.password")
        ));
    }

    private static void validateConfiguration(ConfigLoader config) {
        String dialect = config.get("database.dialect", "toyrdb").trim().toLowerCase(Locale.ROOT);
        if (!"toyrdb".equals(dialect)) {
            throw new IllegalArgumentException("Unsupported ToyRDB database.dialect: " + dialect);
        }

        String ddlAuto = config.get("database.ddl-auto", "none").trim().toLowerCase(Locale.ROOT);
        if (!"none".equals(ddlAuto)) {
            throw new IllegalArgumentException("Unsupported ToyRDB database.ddl-auto: " + ddlAuto);
        }
    }
}
