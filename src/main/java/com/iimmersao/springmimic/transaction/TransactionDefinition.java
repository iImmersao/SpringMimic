package com.iimmersao.springmimic.transaction;

public class TransactionDefinition {
    private final Propagation propagation;
    private final Isolation isolation;
    private final boolean readOnly;

    public TransactionDefinition(Propagation propagation, Isolation isolation, boolean readOnly) {
        this.propagation = propagation == null ? Propagation.REQUIRED : propagation;
        this.isolation = isolation == null ? Isolation.DEFAULT : isolation;
        this.readOnly = readOnly;
    }

    public static TransactionDefinition defaults() {
        return new TransactionDefinition(Propagation.REQUIRED, Isolation.DEFAULT, false);
    }

    public Propagation getPropagation() { return propagation; }
    public Isolation getIsolation() { return isolation; }
    public boolean isReadOnly() { return readOnly; }
}
