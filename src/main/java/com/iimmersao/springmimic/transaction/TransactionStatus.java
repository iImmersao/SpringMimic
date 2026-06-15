package com.iimmersao.springmimic.transaction;

public class TransactionStatus {
    private final TransactionContext context;
    private final boolean newTransaction;
    private boolean completed;

    public TransactionStatus(TransactionContext context, boolean newTransaction) {
        this.context = context;
        this.newTransaction = newTransaction;
    }

    public TransactionContext getContext() { return context; }
    public boolean isNewTransaction() { return newTransaction; }
    public boolean isCompleted() { return completed; }
    public void markCompleted() { this.completed = true; }
    public boolean hasTransaction() { return context != null; }
    public boolean isRollbackOnly() { return context != null && context.isRollbackOnly(); }
    public void setRollbackOnly() {
        if (context != null) {
            context.setRollbackOnly();
        }
    }
}
