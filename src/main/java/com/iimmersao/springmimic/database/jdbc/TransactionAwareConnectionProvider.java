package com.iimmersao.springmimic.database.jdbc;

import com.iimmersao.springmimic.transaction.TransactionSynchronizationManager;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionAwareConnectionProvider implements JdbcConnectionProvider {
    private final JdbcConnectionProvider delegate;

    public TransactionAwareConnectionProvider(JdbcConnectionProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection currentConnection = TransactionSynchronizationManager.getCurrentConnection();
        if (currentConnection != null) {
            return currentConnection;
        }
        return delegate.getConnection();
    }

    @Override
    public void releaseConnection(Connection connection) throws SQLException {
        Connection currentConnection = TransactionSynchronizationManager.getCurrentConnection();
        if (connection != null && connection == currentConnection) {
            return;
        }
        delegate.releaseConnection(connection);
    }
}
