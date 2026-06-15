package com.iimmersao.springmimic.transaction.components;

public interface TransactionalTestService {
    Integer createCommittedUser(String username);
    Integer createUserThenFail(String username);
    Integer createUserThenCheckedFailure(String username) throws TestCheckedException;
    Integer createUserThenRollbackForChecked(String username) throws TestCheckedException;
    Integer createUserThenNoRollbackRuntime(String username);
    Integer createUserThenTimeout(String username);
}