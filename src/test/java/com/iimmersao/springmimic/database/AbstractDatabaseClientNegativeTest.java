package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.repository.RepositoryProxyFactory;
import com.iimmersao.springmimic.web.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractDatabaseClientNegativeTest {

    protected abstract DatabaseClient client();
    protected abstract Class<?> getEntityClass();

    @BeforeEach
    void setup() throws Exception {
        client().deleteAll(getEntityClass());
    }

    @Test
    void shouldReturnEmptyListWhenFilteringOnNonExistentField() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setFilters(Map.of("nonexistent", "value"));

        List<?> users = client().findAll(getEntityClass(), pageRequest);

        assertNotNull(users);
        assertTrue(users.isEmpty(), "Expected empty list for invalid filter");
    }

    @Test
    void shouldThrowExceptionWhenSortingOnInvalidField() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortBy("notARealField");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                client().findAll(getEntityClass(), pageRequest)
        );

        assertTrue(ex.getMessage().contains("Invalid sort field") ||
                        ex.getMessage().contains("No such field"),
                "Unexpected error message: " + ex.getMessage());
    }

    @Test
    void shouldHandleNullFiltersGracefully() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setFilters(null);

        List<?> result = client().findAll(getEntityClass(), pageRequest);
        assertNotNull(result); // Should not throw
    }

    @Test
    void shouldThrowWhenSavingInvalidEntityType() {
        Object wrongType = new Object(); // not a @Table/@Document class
        assertThrows(RuntimeException.class, () ->
                client().save(wrongType)
        );
    }

    @Test
    void shouldThrowWhenCallingDeleteByIdWithWrongType() {
        Object wrongId = "thisShouldBeAnInteger"; // or 123L if expecting Integer
        assertThrows(RuntimeException.class, () -> client().deleteById(getEntityClass(), wrongId));
    }

    abstract class AbstractRepositoryNegativeTest {

        protected abstract Class<?> getEntityClass();
        protected abstract DatabaseClient client();

        protected <T> T createRepository(Class<T> repoClass) {
            RepositoryProxyFactory repositoryProxyFactory = new RepositoryProxyFactory(client());
            return repositoryProxyFactory
                    .createRepository(repoClass, getEntityClass());
        }

        @Test
        void shouldThrowForExistsByWithInvalidField() {
            UserRepository repo = createRepository(UserRepository.class);

            Exception ex = assertThrows(RuntimeException.class, () ->
                    repo.existsByFieldThatDoesNotExist("someValue")
            );

            assertTrue(ex.getMessage().contains("No such field") ||
                            ex.getMessage().contains("not found"),
                    "Unexpected error message: " + ex.getMessage());
        }

        @Test
        void shouldThrowForCountByWithInvalidField() {
            UserRepository repo = createRepository(UserRepository.class);

            Exception ex = assertThrows(RuntimeException.class, () ->
                    repo.countByFieldThatDoesNotExist("someValue")
            );

            assertTrue(ex.getMessage().contains("No such field") ||
                            ex.getMessage().contains("not found"),
                    "Unexpected error message: " + ex.getMessage());
        }

        interface UserRepository {
            boolean existsByFieldThatDoesNotExist(String value);
            long countByFieldThatDoesNotExist(String value);
        }
    }
}
