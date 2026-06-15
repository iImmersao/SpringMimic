package com.iimmersao.springmimic.transaction;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.H2DatabaseClient;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;
import com.iimmersao.springmimic.model.H2User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTransactionManagerTest {
    private H2DatabaseClient client;
    private JdbcTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        ConfigLoader config = new ConfigLoader("h2");
        JdbcConnectionProvider rawProvider = new DriverManagerConnectionProvider(
                config.get("h2.url"),
                config.get("h2.username"),
                config.get("h2.password")
        );
        client = new H2DatabaseClient(config, new TransactionAwareConnectionProvider(rawProvider));
        transactionManager = new JdbcTransactionManager(rawProvider);
        client.deleteAll(H2User.class);
        TransactionSynchronizationManager.clear();
    }

    @Test
    void shouldCommitNewTransaction() {
        TransactionStatus status = transactionManager.begin(TransactionDefinition.defaults());
        H2User user = new H2User();
        user.setUsername("committed");
        user.setEmail("committed@example.com");

        client.save(user);
        transactionManager.commit(status);

        Optional<H2User> found = client.findById(H2User.class, user.getId());
        assertTrue(found.isPresent());
        assertEquals("committed", found.get().getUsername());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void shouldRollbackNewTransaction() {
        TransactionStatus status = transactionManager.begin(TransactionDefinition.defaults());
        H2User user = new H2User();
        user.setUsername("rolledback");
        user.setEmail("rolledback@example.com");

        client.save(user);
        transactionManager.rollback(status);

        assertTrue(client.findById(H2User.class, user.getId()).isEmpty());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void requiredTransactionShouldJoinExistingTransaction() {
        TransactionStatus outer = transactionManager.begin(TransactionDefinition.defaults());
        TransactionStatus inner = transactionManager.begin(TransactionDefinition.defaults());
        H2User user = new H2User();
        user.setUsername("joined");
        user.setEmail("joined@example.com");

        client.save(user);
        transactionManager.commit(inner);
        assertTrue(TransactionSynchronizationManager.isTransactionActive());

        transactionManager.rollback(outer);
        assertTrue(client.findById(H2User.class, user.getId()).isEmpty());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }
}
