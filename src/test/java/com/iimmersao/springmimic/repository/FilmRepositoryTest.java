package com.iimmersao.springmimic.repository;

import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.database.H2DatabaseClient;
import com.iimmersao.springmimic.model.Film;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmRepositoryTest {

    private static DatabaseClient client;
    private static FilmRepository filmRepository;
    private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "password";

    @BeforeAll
    static void setUp() throws IOException, SQLException {
        ConfigLoader configLoader = new ConfigLoader("application-h2.properties");  // load from test config or defaults
        client = new H2DatabaseClient(configLoader); // or MySqlDatabaseClient, etc.

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE film (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL
                );
            """);
        }

        RepositoryProxyFactory factory = new RepositoryProxyFactory(client);
        filmRepository = factory.createRepository(FilmRepository.class, Film.class);
    }

    @BeforeEach
    void cleanUp() {
        client.deleteAll(Film.class);
    }

    @Test
    void shouldSaveAndFindByTitle() {
        Film film = new Film(null, "The Matrix");
        filmRepository.save(film);

        Film result = filmRepository.findByTitle("The Matrix");
        assertNotNull(result);
        assertEquals("The Matrix", result.getTitle());
    }

    @Test
    void shouldFindByTitleContains() {
        filmRepository.save(new Film(null, "The Matrix"));
        filmRepository.save(new Film(null, "Matrix Reloaded"));
        filmRepository.save(new Film(null, "John Wick"));

        List<Film> results = filmRepository.findByTitleContains("Matrix");
        assertEquals(2, results.size());
    }

    @Test
    void shouldDeleteById() {
        Film film = new Film(null, "Delete Me");
        filmRepository.save(film);

        assertNotNull(film.getId());

        filmRepository.deleteById(film.getId());

        Film result = filmRepository.findByTitle("Delete Me");
        assertNull(result);
    }

    @Test
    void shouldReturnAllFilms() {
        filmRepository.save(new Film(null, "Film A"));
        filmRepository.save(new Film(null, "Film B"));

        List<Film> films = filmRepository.findAll();
        assertTrue(films.size() >= 2); // assumes DB is cleared/reset between tests
    }
}
