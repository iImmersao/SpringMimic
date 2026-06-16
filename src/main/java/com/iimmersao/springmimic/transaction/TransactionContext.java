package com.iimmersao.springmimic.transaction;

import java.sql.Connection;
import java.time.Duration;

public class TransactionContext {
    private final Connection connection;
    private final boolean previousAutoCommit;
    private final int previousIsolation;
    private final boolean previousReadOnly;
    private final boolean readOnly;
    private final Isolation isolation;
    private final long startNanos;
    private final int timeoutSeconds;
    private boolean rollbackOnly;

    public TransactionContext(
            Connection connection,
            boolean previousAutoCommit,
            int previousIsolation,
            boolean previousReadOnly,
            boolean readOnly,
            Isolation isolation,
            int timeoutSeconds) {
        this.connection = connection;
        this.previousAutoCommit = previousAutoCommit;
        this.previousIsolation = previousIsolation;
        this.previousReadOnly = previousReadOnly;
        this.readOnly = readOnly;
        this.isolation = isolation == null ? Isolation.DEFAULT : isolation;
        this.timeoutSeconds = timeoutSeconds;
        this.startNanos = System.nanoTime();
    }

    public Connection getConnection() { return connection; }
    public boolean getPreviousAutoCommit() { return previousAutoCommit; }
    public int getPreviousIsolation() { return previousIsolation; }
    public boolean getPreviousReadOnly() { return previousReadOnly; }
    public boolean isReadOnly() { return readOnly; }
    public Isolation getIsolation() { return isolation; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public boolean isRollbackOnly() { return rollbackOnly; }
    public void setRollbackOnly() { this.rollbackOnly = true; }

    public boolean hasTimeout() {
        return timeoutSeconds > 0;
    }

    public boolean isTimedOut() {
        return hasTimeout() && getElapsed().compareTo(Duration.ofSeconds(timeoutSeconds)) >= 0;
    }

    public Duration getElapsed() {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
