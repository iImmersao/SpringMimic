package com.iimmersao.springmimic.transaction;

import com.iimmersao.springmimic.core.ApplicationContext;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.H2DatabaseClient;
import com.iimmersao.springmimic.database.jdbc.DriverManagerConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.JdbcConnectionProvider;
import com.iimmersao.springmimic.database.jdbc.TransactionAwareConnectionProvider;
import com.iimmersao.springmimic.model.H2User;
import com.iimmersao.springmimic.transaction.components.OuterTransactionService;
import com.iimmersao.springmimic.transaction.components.RequiresNewService;
import com.iimmersao.springmimic.transaction.components.TestCheckedException;
import com.iimmersao.springmimic.transaction.components.TransactionalConsumer;
import com.iimmersao.springmimic.transaction.components.TransactionalTestService;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeclarativeTransactionTest {
    private H2DatabaseClient client;
    private ApplicationContext context;
    private TransactionalTestService service;
    private RequiresNewService requiresNewService;
    private OuterTransactionService outerTransactionService;

    @BeforeEach
    void setUp() {
        ConfigLoader config = new ConfigLoader("h2");
        JdbcConnectionProvider rawProvider = new DriverManagerConnectionProvider(
                config.get("h2.url"),
                config.get("h2.username"),
                config.get("h2.password")
        );
        client = new H2DatabaseClient(config, new TransactionAwareConnectionProvider(rawProvider));
        client.deleteAll(H2User.class);
        TransactionSynchronizationManager.clear();

        context = new ApplicationContext("com.iimmersao.springmimic.transaction.components");
        context.registerDatabaseBean(DatabaseClient.class, client);
        context.registerBean(TransactionManager.class, new JdbcTransactionManager(rawProvider));
        context.initialize(null);
        context.injectDependencies();
        service = context.getBean(TransactionalTestService.class);
        requiresNewService = context.getBean(RequiresNewService.class);
        outerTransactionService = context.getBean(OuterTransactionService.class);
    }

    @Test
    void shouldReturnTransactionalProxyForInterfaceBean() {
        assertTrue(Proxy.isProxyClass(service.getClass()));
    }

    @Test
    void shouldCommitWhenTransactionalMethodSucceeds() {
        Integer id = service.createCommittedUser("committed-declarative");

        assertTrue(client.findById(H2User.class, id).isPresent());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void shouldRollbackRuntimeExceptionByDefault() {
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> service.createUserThenFail("rollback-runtime")
        );

        Integer id = parseTrailingId(thrown.getMessage());
        assertTrue(client.findById(H2User.class, id).isEmpty());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void shouldCommitCheckedExceptionByDefault() {
        TestCheckedException thrown = assertThrows(
                TestCheckedException.class,
                () -> service.createUserThenCheckedFailure("commit-checked")
        );

        Integer id = parseTrailingId(thrown.getMessage());
        assertTrue(client.findById(H2User.class, id).isPresent());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void shouldRollbackCheckedExceptionWhenConfigured() {
        TestCheckedException thrown = assertThrows(
                TestCheckedException.class,
                () -> service.createUserThenRollbackForChecked("rollback-checked")
        );

        Integer id = parseTrailingId(thrown.getMessage());
        assertTrue(client.findById(H2User.class, id).isEmpty());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void shouldCommitRuntimeExceptionWhenNoRollbackForMatches() {
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> service.createUserThenNoRollbackRuntime("commit-runtime")
        );

        Integer id = parseTrailingId(thrown.getMessage());
        assertTrue(client.findById(H2User.class, id).isPresent());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void shouldInjectTransactionalProxyIntoInterfaceField() {
        TransactionalConsumer consumer = context.getBean(TransactionalConsumer.class);

        assertTrue(consumer.hasProxyService());
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> consumer.createUserThenFail("consumer-rollback")
        );

        Integer id = parseTrailingId(thrown.getMessage());
        assertTrue(client.findById(H2User.class, id).isEmpty());
    }

    @Test
    void requiresNewShouldCommitInnerTransactionWhenOuterRollsBack() {
        assertThrows(
                IllegalStateException.class,
                () -> outerTransactionService.createOuterThenRequiresNewThenFail("outer-rollback", "inner-commit")
        );

        assertTrue(findByUsername("outer-rollback").isEmpty());
        assertEquals(1, findByUsername("inner-commit").size());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void supportsShouldRunWithoutTransactionWhenNoneExists() {
        assertFalse(requiresNewService.supportsHasActiveTransaction());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void supportsShouldJoinActiveTransactionWhenOneExists() {
        assertTrue(requiresNewService.requiredCallingSupportsHasActiveTransaction());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void readOnlyShouldBeAppliedToTransactionalConnection() {
        assertTrue(requiresNewService.readOnlyFlagIsApplied());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    @Test
    void isolationShouldBeAppliedToTransactionalConnection() {
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, requiresNewService.serializableIsolationLevel());
        assertFalse(TransactionSynchronizationManager.isTransactionActive());
    }

    private List<H2User> findByUsername(String username) {
        PageRequest request = new PageRequest();
        request.setFilters(Map.of("username", username));
        return client.findAll(H2User.class, request);
    }

    private Integer parseTrailingId(String message) {
        return Integer.parseInt(message.substring(message.lastIndexOf(' ') + 1));
    }
}