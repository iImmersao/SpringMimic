package com.iimmersao.springmimic.transaction.components;

public interface OuterTransactionService {
    void createOuterThenRequiresNewThenFail(String outerUsername, String innerUsername);
}