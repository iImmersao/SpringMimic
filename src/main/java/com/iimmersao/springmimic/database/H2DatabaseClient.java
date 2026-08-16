package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.dialect.H2Dialect;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;
import com.iimmersao.springmimic.exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Bean
public class H2DatabaseClient extends JdbcDatabaseClient {

    private final JdbcConnectionProvider connectionProvider;

    public H2DatabaseClient(ConfigLoader config) {
        this(config, new TransactionAwareConnectionProvider(new DriverManagerConnectionProvider(
                config.get("h2.url"),
                config.get("h2.username"),
                config.get("h2.password")
        )));
    }

    public H2DatabaseClient(ConfigLoader config, JdbcConnectionProvider connectionProvider) {
        super(connectionProvider, new H2Dialect());
        this.connectionProvider = connectionProvider;
        DriverCheck.loadDriver(config, "h2");
        initializeSchema();
    }

    private void initializeSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) NOT NULL,
                    email VARCHAR(255),
                    active BOOLEAN,
                    age INT
                );
                """;

        Connection conn = null;
        try {
            conn = connectionProvider.getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize H2 schema", e);
        } finally {
            releaseConnection(conn);
        }
    }

    private void releaseConnection(Connection connection) {
        try {
            connectionProvider.releaseConnection(connection);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to release H2 connection", e);
        }
    }
}
