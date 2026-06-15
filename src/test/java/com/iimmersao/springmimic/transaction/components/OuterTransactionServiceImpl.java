package com.iimmersao.springmimic.transaction.components;

import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.annotations.Service;
import com.iimmersao.springmimic.annotations.Transactional;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.model.H2User;

@Service
public class OuterTransactionServiceImpl implements OuterTransactionService {
    @Inject
    private DatabaseClient databaseClient;

    @Inject
    private RequiresNewService requiresNewService;

    @Override
    @Transactional
    public void createOuterThenRequiresNewThenFail(String outerUsername, String innerUsername) {
        Integer innerId = requiresNewService.createRequiresNewUser(innerUsername);
        saveUser(outerUsername);
        throw new IllegalStateException("outer rollback after inner " + innerId);
    }

    private void saveUser(String username) {
        H2User user = new H2User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        databaseClient.save(user);
    }
}