package com.iimmersao.springmimic.transaction;

public interface TransactionManager {
    TransactionStatus begin(TransactionDefinition definition);
    void commit(TransactionStatus status);
    void rollback(TransactionStatus status);
}
