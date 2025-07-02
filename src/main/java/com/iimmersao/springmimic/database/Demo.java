package com.iimmersao.springmimic.database;

// Example usage (e.g., in Main.java or a test)
import com.iimmersao.springmimic.model.User;
import com.iimmersao.springmimic.database.MySqlDatabaseClient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;

public class Demo {

    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/sakila", "root", "Basement99!");

        MySqlDatabaseClient client = new MySqlDatabaseClient(conn);

        // Save a new user
        User user = new User("johndoe5", "john5@example.com");
        client.save(user);
        System.out.println("User id: " + user.getId());

        // Find user by ID
        Optional<User> found = client.findById(User.class, user.getId());
        found.ifPresent(System.out::println);

        // Update user
        //user.setEmail("new-email@example.com");
        user.setEmail("new" + user.getEmail());
        client.updateById(user);

        // Delete user
        client.deleteById(User.class, user.getId() - 1);
    }
}
