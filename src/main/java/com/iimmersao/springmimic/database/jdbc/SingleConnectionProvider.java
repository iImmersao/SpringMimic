package com.iimmersao.springmimic.database.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class SingleConnectionProvider implements JdbcConnectionProvider {
    private final Connection connection;

    public SingleConnectionProvider(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void releaseConnection(Connection connection) throws SQLException {
        // The owner of the supplied connection controls its lifecycle.
    }
}
