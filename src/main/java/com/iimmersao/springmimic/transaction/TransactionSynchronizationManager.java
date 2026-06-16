package com.iimmersao.springmimic.transaction;

import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.Deque;

public final class TransactionSynchronizationManager {
    private static final ThreadLocal<Deque<TransactionContext>> CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private TransactionSynchronizationManager() {}

    public static boolean isTransactionActive() {
        return !CONTEXTS.get().isEmpty();
    }

    public static TransactionContext getCurrentContext() {
        return CONTEXTS.get().peek();
    }

    public static Connection getCurrentConnection() {
        TransactionContext context = getCurrentContext();
        return context == null ? null : context.getConnection();
    }

    public static void bind(TransactionContext context) {
        CONTEXTS.get().push(context);
    }

    public static void unbind() {
        Deque<TransactionContext> contexts = CONTEXTS.get();
        contexts.pop();
        if (contexts.isEmpty()) {
            CONTEXTS.remove();
        }
    }

    public static void clear() {
        CONTEXTS.remove();
    }
}
