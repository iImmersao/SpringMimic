package com.iimmersao.springmimic.transaction;

import java.sql.Connection;

public class TransactionContext {
    private final Connection connection;
    private final boolean previousAutoCommit;
    private final int previousIsolation;
    private final boolean previousReadOnly;
    private boolean rollbackOnly;

    public TransactionContext(Connection connection, boolean previousAutoCommit, int previousIsolation, boolean previousReadOnly) {
        this.connection = connection;
        this.previousAutoCommit = previousAutoCommit;
        this.previousIsolation = previousIsolation;
        this.previousReadOnly = previousReadOnly;
    }

    public Connection getConnection() { return connection; }
    public boolean getPreviousAutoCommit() { return previousAutoCommit; }
    public int getPreviousIsolation() { return previousIsolation; }
    public boolean getPreviousReadOnly() { return previousReadOnly; }
    public boolean isRollbackOnly() { return rollbackOnly; }
    public void setRollbackOnly() { this.rollbackOnly = true; }
}
