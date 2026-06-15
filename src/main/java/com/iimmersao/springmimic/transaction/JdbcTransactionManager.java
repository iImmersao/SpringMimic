package com.iimmersao.springmimic.transaction;

import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTransactionManager implements TransactionManager {
    private final JdbcConnectionProvider connectionProvider;
    private final int defaultTimeoutSeconds;

    public JdbcTransactionManager(JdbcConnectionProvider connectionProvider) {
        this(connectionProvider, TransactionDefinition.TIMEOUT_DEFAULT);
    }

    public JdbcTransactionManager(JdbcConnectionProvider connectionProvider, int defaultTimeoutSeconds) {
        if (defaultTimeoutSeconds < TransactionDefinition.TIMEOUT_DEFAULT) {
            throw new IllegalArgumentException("defaultTimeoutSeconds must be -1 or greater");
        }
        this.connectionProvider = connectionProvider;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    @Override
    public TransactionStatus begin(TransactionDefinition definition) {
        TransactionDefinition txDefinition = definition == null ? TransactionDefinition.defaults() : definition;
        TransactionContext current = TransactionSynchronizationManager.getCurrentContext();

        if (txDefinition.getPropagation() == Propagation.SUPPORTS && current == null) {
            return new TransactionStatus(null, false);
        }

        if (current != null && txDefinition.getPropagation() != Propagation.REQUIRES_NEW) {
            return new TransactionStatus(current, false);
        }

        return beginNewTransaction(txDefinition);
    }

    @Override
    public void commit(TransactionStatus status) {
        validateOpenStatus(status);
        if (!status.hasTransaction()) {
            status.markCompleted();
            return;
        }
        if (!status.isNewTransaction()) {
            if (status.getContext().isTimedOut()) {
                status.setRollbackOnly();
            }
            status.markCompleted();
            return;
        }

        TransactionContext context = status.getContext();
        try {
            if (context.isTimedOut()) {
                context.setRollbackOnly();
                context.getConnection().rollback();
                throw new TransactionTimeoutException(timeoutMessage(context));
            }
            if (context.isRollbackOnly()) {
                context.getConnection().rollback();
            } else {
                context.getConnection().commit();
            }
        } catch (TransactionTimeoutException e) {
            throw e;
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

    private TransactionStatus beginNewTransaction(TransactionDefinition txDefinition) {
        try {
            Connection connection = connectionProvider.getConnection();
            boolean previousAutoCommit = connection.getAutoCommit();
            int previousIsolation = connection.getTransactionIsolation();
            boolean previousReadOnly = connection.isReadOnly();
            int timeoutSeconds = resolveTimeoutSeconds(txDefinition);

            if (txDefinition.getIsolation() != Isolation.DEFAULT) {
                connection.setTransactionIsolation(txDefinition.getIsolation().toJdbcLevel(previousIsolation));
            }
            if (connection.isReadOnly() != txDefinition.isReadOnly()) {
                connection.setReadOnly(txDefinition.isReadOnly());
            }
            if (previousAutoCommit) {
                connection.setAutoCommit(false);
            }

            TransactionContext context = new TransactionContext(
                    connection,
                    previousAutoCommit,
                    previousIsolation,
                    previousReadOnly,
                    txDefinition.isReadOnly(),
                    txDefinition.getIsolation(),
                    timeoutSeconds
            );
            TransactionSynchronizationManager.bind(context);
            return new TransactionStatus(context, true);
        } catch (SQLException e) {
            throw new TransactionException("Failed to begin JDBC transaction", e);
        }
    }

    private int resolveTimeoutSeconds(TransactionDefinition txDefinition) {
        if (txDefinition.getTimeoutSeconds() != TransactionDefinition.TIMEOUT_DEFAULT) {
            return txDefinition.getTimeoutSeconds();
        }
        return defaultTimeoutSeconds;
    }

    private String timeoutMessage(TransactionContext context) {
        return "JDBC transaction timed out after " + context.getTimeoutSeconds()
                + " second(s); elapsed " + context.getElapsed().toMillis() + " ms";
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