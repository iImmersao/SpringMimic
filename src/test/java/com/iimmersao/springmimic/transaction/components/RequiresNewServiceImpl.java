package com.iimmersao.springmimic.transaction.components;

import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.annotations.Service;
import com.iimmersao.springmimic.annotations.Transactional;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.model.H2User;
import com.iimmersao.springmimic.transaction.Isolation;
import com.iimmersao.springmimic.transaction.Propagation;
import com.iimmersao.springmimic.transaction.TransactionSynchronizationManager;

import java.sql.Connection;
import java.sql.SQLException;

@Service
public class RequiresNewServiceImpl implements RequiresNewService {
    @Inject
    private DatabaseClient databaseClient;

    @Inject
    private RequiresNewService self;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer createRequiresNewUser(String username) {
        return saveUser(username);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean supportsHasActiveTransaction() {
        return TransactionSynchronizationManager.isTransactionActive();
    }

    @Override
    @Transactional
    public boolean requiredCallingSupportsHasActiveTransaction() {
        return self.supportsHasActiveTransaction();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean readOnlyFlagIsApplied() {
        return TransactionSynchronizationManager.getCurrentContext().isReadOnly();
    }

    @Override
    @SuppressWarnings("resource")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public int serializableIsolationLevel() {
        try {
            return currentConnection().getTransactionIsolation();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to inspect isolation", e);
        }
    }

    private Integer saveUser(String username) {
        H2User user = new H2User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        databaseClient.save(user);
        return user.getId();
    }

    private Connection currentConnection() {
        Connection connection = TransactionSynchronizationManager.getCurrentConnection();
        if (connection == null) {
            throw new IllegalStateException("No active transaction connection");
        }
        return connection;
    }
}
