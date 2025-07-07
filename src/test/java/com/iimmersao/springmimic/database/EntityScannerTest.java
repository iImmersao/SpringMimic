package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.model.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EntityScannerTest {

    @Test
    public void shouldDetectUserEntity() {
        EntityScanner scanner = new EntityScanner();
        Set<Class<?>> entities = scanner.scanEntities("com.iimmersao.springmimic.model");

        assertNotNull(entities);
        assertTrue(entities.contains(User.class), "User entity should be detected");
    }
}
