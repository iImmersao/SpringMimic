package com.iimmersao.springmimic.database;

// Example usage (e.g., in Main.java or a test)
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.MySqlUser;

import java.util.Optional;

public class Demo {

    public static void main(String[] args) throws Exception {
//        Connection conn = DriverManager.getConnection(
//                "jdbc:mysql://localhost:3306/sakila", "root", "Basement99!");

        ConfigLoader config = new ConfigLoader("application.properties");
        MySqlDatabaseClient client = new MySqlDatabaseClient(config);

        // Save a new user
        MySqlUser user = new MySqlUser("johndoe5", "john5@example.com");
        client.save(user);
        System.out.println("User id: " + user.getId());

        // Find user by ID
        Optional<MySqlUser> found = client.findById(MySqlUser.class, user.getId());
        found.ifPresent(System.out::println);

        // Update user
        //user.setEmail("new-email@example.com");
        user.setEmail("new" + user.getEmail());
        client.updateById(user);

        // Delete user
        client.deleteById(MySqlUser.class, user.getId() - 1);
    }
}
