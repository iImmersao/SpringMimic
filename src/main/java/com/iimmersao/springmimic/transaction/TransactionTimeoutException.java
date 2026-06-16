package com.iimmersao.springmimic.transaction;

public class TransactionTimeoutException extends TransactionException {
    public TransactionTimeoutException(String message) {
        super(message);
    }
}
