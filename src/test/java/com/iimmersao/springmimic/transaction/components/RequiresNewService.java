package com.iimmersao.springmimic.transaction.components;

public interface RequiresNewService {
    Integer createRequiresNewUser(String username);
    boolean supportsHasActiveTransaction();
    boolean requiredCallingSupportsHasActiveTransaction();
    boolean readOnlyFlagIsApplied();
    int serializableIsolationLevel();
}