package com.iimmersao.springmimic.transaction.components;

public interface TransactionalTestService {
    Integer createCommittedUser(String username);
    void createUserThenFail(String username);
    void createUserThenCheckedFailure(String username) throws TestCheckedException;
    void createUserThenRollbackForChecked(String username) throws TestCheckedException;
    void createUserThenNoRollbackRuntime(String username);
    void createUserThenTimeout(String username);
}
