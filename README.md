# Spring Mimic Framework

A lightweight, annotation-driven Java framework inspired by Spring Boot — designed to simplify web development with minimal dependencies. Built to support RESTful APIs, dependency injection, database access (MySQL, MongoDB, H2), component scanning, authentication, and more.

---

## 🚀 Features

- ✅ Annotation-based routing (`@GetMapping`, `@PostMapping`, etc.)
- ✅ Dependency Injection with `@Component`, `@Inject`, etc.
- ✅ In-memory web server (NanoHTTPD)
- ✅ Built-in support for:
  - **MySQL**, **MongoDB**, and **H2** database access
  - CRUD-style dynamic repository interfaces (`CrudRepository`)
  - Pagination, filtering, sorting
  - Declarative role-based access control via `@Authenticated` and `@RolesAllowed`
- ✅ Component scanning with `@SpringMimicApplication` and `@ComponentScan`
- ✅ Basic authentication support
- ✅ Easily testable via JUnit with database clients and mock HTTP support

---

## 📦 Getting Started

### 1. Include in Your Project

Install the framework locally:

```bash
mvn clean install


<dependency>
    <groupId>com.iimmersao</groupId>
    <artifactId>springmimic</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Define an Entry Point

```bash
@SpringMimicApplication
@ComponentScan("com.example.myapp")
public class Main {
    public static void main(String[] args) {
        SpringMimicApplication.run(Main.class);
    }
}
```

### 3. Create a Controller

```bash
@RestController
@RequestMapping("/users")
public class UserController {

    @Inject
    private UserService userService;

    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") String id) {
        return userService.findById(id).orElseThrow();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.save(user);
    }
}
```

### 4. Define a Repository

```bash
@Repository
public interface UserRepository extends CrudRepository<User, String> {
    List<User> findByUsername(String username);
    List<User> findByEmailContains(String keyword);
    boolean existsByUsername(String username);
}
```

### 5. Add an Entity

```bash
@Entity
@Table(name = "users")
public class User {
    @Id
    private String id;

    private String username;
    private String email;

    // getters and setters
}
```

### 6. Configure application.properties

```
# Choose an application name and the port that the application will use
server.name=UserApplication
server.port=8081

# Set up the logging
logging.level=DEBUG
logging.output=file   # or 'console' or 'both'
logging.file=logs/custom.log

# Specify the location from which web content such as HTML will be served
static.path=public

# Configure a database type - e.g., MongoDB
db.type=mongodb
#db.type=h2
#db.type=mysql

# MongoDB
mongodb.uri=mongodb://localhost:27017
mongodb.database=myappmongodb

# MySQL
mysql.url=jdbc:mysql://localhost:3306/myappmysqldb
mysql.username=root
mysql.password=secret

# H2 (default for testing)
h2.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
h2.username=sa
h2.password=password
```

🧪 Testing Support

    Unit tests for:

        Controllers via embedded HTTP client

        Repositories and DB clients (MySQL, Mongo, H2)

        Negative cases (invalid filters, sort fields, etc.)

    Mock authentication available for isolated tests

🧰 Requirements

    Java 21+

    Maven 3.8+

