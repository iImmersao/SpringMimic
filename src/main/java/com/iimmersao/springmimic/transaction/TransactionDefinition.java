package com.iimmersao.springmimic.transaction;

public class TransactionDefinition {
    public static final int TIMEOUT_DEFAULT = -1;

    private final Propagation propagation;
    private final Isolation isolation;
    private final boolean readOnly;
    private final int timeoutSeconds;

    public TransactionDefinition(Propagation propagation, Isolation isolation, boolean readOnly) {
        this(propagation, isolation, readOnly, TIMEOUT_DEFAULT);
    }

    public TransactionDefinition(Propagation propagation, Isolation isolation, boolean readOnly, int timeoutSeconds) {
        if (timeoutSeconds < TIMEOUT_DEFAULT) {
            throw new IllegalArgumentException("timeoutSeconds must be -1 or greater");
        }
        this.propagation = propagation == null ? Propagation.REQUIRED : propagation;
        this.isolation = isolation == null ? Isolation.DEFAULT : isolation;
        this.readOnly = readOnly;
        this.timeoutSeconds = timeoutSeconds;
    }

    public static TransactionDefinition defaults() {
        return new TransactionDefinition(Propagation.REQUIRED, Isolation.DEFAULT, false);
    }

    public Propagation getPropagation() { return propagation; }
    public Isolation getIsolation() { return isolation; }
    public boolean isReadOnly() { return readOnly; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
}
