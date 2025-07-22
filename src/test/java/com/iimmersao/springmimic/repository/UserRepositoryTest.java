package com.iimmersao.springmimic.repository;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.H2DatabaseClient;
import com.iimmersao.springmimic.model.BaseUserEntity;
import com.iimmersao.springmimic.model.H2UserEntity;
import com.iimmersao.springmimic.model.UserDTO;
import com.iimmersao.springmimic.testcomponents.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings(value = "unused")
public class UserRepositoryTest {

    private static DatabaseClient client;
    private static UserRepository userRepository;
    private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "password";

    @BeforeAll
    static void setUp() throws SQLException {
        ConfigLoader configLoader = new ConfigLoader("h2");  // load from test config or defaults
        client = new H2DatabaseClient(configLoader); // or MySqlDatabaseClient, etc.

        String sql = """
        CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(100) NOT NULL,
            email VARCHAR(255),
            active BOOLEAN,
            age INT
        );
    """;

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }

        RepositoryProxyFactory factory = new RepositoryProxyFactory(client);
        userRepository = factory.createRepository(UserRepository.class, H2UserEntity.class);
    }

    @BeforeEach
    void cleanUp() {
        client.deleteAll(UserDTO.class);
    }

    @Test
    void shouldSaveAndFindByUsername() {
        H2UserEntity user = new H2UserEntity(null, "John", "john@example.com");
        userRepository.save(user);

        BaseUserEntity result = userRepository.findByUsername("John");
        assertNotNull(result);
        assertEquals("John", result.getUsername());
    }
}
