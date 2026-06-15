package com.iimmersao.springmimic.transaction.components;

import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.annotations.Service;
import com.iimmersao.springmimic.annotations.Transactional;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.model.H2User;

@Service
public class TransactionalTestServiceImpl implements TransactionalTestService {
    @Inject
    private DatabaseClient databaseClient;

    @Override
    @Transactional
    public Integer createCommittedUser(String username) {
        return saveUser(username);
    }

    @Override
    @Transactional
    public Integer createUserThenFail(String username) {
        Integer id = saveUser(username);
        throw new IllegalStateException("boom after " + id);
    }

    @Override
    @Transactional
    public Integer createUserThenCheckedFailure(String username) throws TestCheckedException {
        Integer id = saveUser(username);
        throw new TestCheckedException("checked after " + id);
    }

    @Override
    @Transactional(rollbackFor = TestCheckedException.class)
    public Integer createUserThenRollbackForChecked(String username) throws TestCheckedException {
        Integer id = saveUser(username);
        throw new TestCheckedException("checked rollback after " + id);
    }

    @Override
    @Transactional(noRollbackFor = IllegalStateException.class)
    public Integer createUserThenNoRollbackRuntime(String username) {
        Integer id = saveUser(username);
        throw new IllegalStateException("committed after " + id);
    }

    private Integer saveUser(String username) {
        H2User user = new H2User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        databaseClient.save(user);
        return user.getId();
    }
}