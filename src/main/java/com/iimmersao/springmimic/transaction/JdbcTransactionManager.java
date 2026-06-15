package com.iimmersao.springmimic.transaction;

import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTransactionManager implements TransactionManager {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcTransactionManager(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public TransactionStatus begin(TransactionDefinition definition) {
        TransactionDefinition txDefinition = definition == null ? TransactionDefinition.defaults() : definition;
        TransactionContext current = TransactionSynchronizationManager.getCurrentContext();

        if (txDefinition.getPropagation() == Propagation.SUPPORTS && current == null) {
            return new TransactionStatus(null, false);
        }

        if (txDefinition.getPropagation() == Propagation.REQUIRES_NEW) {
            throw new TransactionException("REQUIRES_NEW propagation is not implemented in the JDBC transaction foundation yet");
        }

        if (current != null) {
            return new TransactionStatus(current, false);
        }

        try {
            Connection connection = connectionProvider.getConnection();
            boolean previousAutoCommit = connection.getAutoCommit();
            int previousIsolation = connection.getTransactionIsolation();
            boolean previousReadOnly = connection.isReadOnly();

            if (txDefinition.getIsolation() != Isolation.DEFAULT) {
                connection.setTransactionIsolation(txDefinition.getIsolation().toJdbcLevel(previousIsolation));
            }
            if (connection.isReadOnly() != txDefinition.isReadOnly()) {
                connection.setReadOnly(txDefinition.isReadOnly());
            }
            if (previousAutoCommit) {
                connection.setAutoCommit(false);
            }

            TransactionContext context = new TransactionContext(connection, previousAutoCommit, previousIsolation, previousReadOnly);
            TransactionSynchronizationManager.bind(context);
            return new TransactionStatus(context, true);
        } catch (SQLException e) {
            throw new TransactionException("Failed to begin JDBC transaction", e);
        }
    }

    @Override
    public void commit(TransactionStatus status) {
        validateOpenStatus(status);
        if (!status.hasTransaction()) {
            status.markCompleted();
            return;
        }
        if (!status.isNewTransaction()) {
            status.markCompleted();
            return;
        }

        TransactionContext context = status.getContext();
        try {
            if (context.isRollbackOnly()) {
                context.getConnection().rollback();
            } else {
                context.getConnection().commit();
            }
        } catch (SQLException e) {
            throw new TransactionException("Failed to commit JDBC transaction", e);
        } finally {
            cleanup(status);
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        validateOpenStatus(status);
        if (!status.hasTransaction()) {
            status.markCompleted();
            return;
        }
        if (!status.isNewTransaction()) {
            status.setRollbackOnly();
            status.markCompleted();
            return;
        }

        try {
            status.getContext().getConnection().rollback();
        } catch (SQLException e) {
            throw new TransactionException("Failed to roll back JDBC transaction", e);
        } finally {
            cleanup(status);
        }
    }

    private void cleanup(TransactionStatus status) {
        TransactionContext context = status.getContext();
        RuntimeException cleanupFailure = null;
        try {
            Connection connection = context.getConnection();
            connection.setTransactionIsolation(context.getPreviousIsolation());
            connection.setReadOnly(context.getPreviousReadOnly());
            connection.setAutoCommit(context.getPreviousAutoCommit());
        } catch (SQLException e) {
            cleanupFailure = new TransactionException("Failed to restore JDBC connection state", e);
        }

        try {
            TransactionSynchronizationManager.unbind();
        } catch (RuntimeException e) {
            if (cleanupFailure == null) {
                cleanupFailure = e;
            }
        }

        try {
            connectionProvider.releaseConnection(context.getConnection());
        } catch (SQLException e) {
            if (cleanupFailure == null) {
                cleanupFailure = new TransactionException("Failed to release JDBC transaction connection", e);
            }
        } finally {
            status.markCompleted();
        }

        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }

    private void validateOpenStatus(TransactionStatus status) {
        if (status == null) {
            throw new TransactionException("Transaction status must not be null");
        }
        if (status.isCompleted()) {
            throw new TransactionException("Transaction is already completed");
        }
    }
}
