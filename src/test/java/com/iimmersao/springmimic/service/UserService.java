package com.iimmersao.springmimic.service;

import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.annotations.Service;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.model.BaseUserEntity;
import com.iimmersao.springmimic.model.UserDTO;
import com.iimmersao.springmimic.model.UserMapper;
import com.iimmersao.springmimic.model.H2UserEntity;
import com.iimmersao.springmimic.model.MySqlUserEntity;
import com.iimmersao.springmimic.model.MongoUserEntity;
import com.iimmersao.springmimic.testcomponents.H2UserRepository;
import com.iimmersao.springmimic.testcomponents.MongoUserRepository;
import com.iimmersao.springmimic.testcomponents.MySqlUserRepository;
import com.iimmersao.springmimic.testcomponents.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Inject
    private DatabaseClient databaseClient;

    private UserRepository userRepository;

    private ConfigLoader config;

    private String dbType;

    public UserService(
            MySqlUserRepository mySqlUserRepository,
            H2UserRepository h2UserRepository,
            MongoUserRepository mongoUserRepository,
            ConfigLoader config) {

        this.config = config;
        this.dbType = getDbType();
        if (mySqlUserRepository != null && "mysql".equals(dbType)) {
            this.userRepository = mySqlUserRepository;
        } else if (h2UserRepository != null && "h2".equals(dbType)) {
            this.userRepository = h2UserRepository;
        } else if (mongoUserRepository != null && "mongodb".equals(dbType)) {
            this.userRepository = mongoUserRepository;
        } else {
            throw new IllegalStateException("No suitable UserRepository found. Check your database configuration.");
        }
    }

    private String getDbType() {
        return config.get("db.type", "mysql").trim().toLowerCase();
    }

    private Class getEntityClass() {
        return switch (getDbType()) {
            case "mysql" -> MySqlUserEntity.class;
            case "h2" -> H2UserEntity.class;
            case "mongodb" -> MongoUserEntity.class;
            default -> throw new IllegalStateException("Unsupported db.type: " + getDbType());
        };
    }

//    public User save(User dto) {
//        Object entity = UserMapper.toEntity(dto, getDbType());
//        databaseClient.save(entity);
//        return UserMapper.toDTO(entity);
//    }

    public UserDTO save(UserDTO dto) {
        BaseUserEntity entity = UserMapper.toEntity(dto, getDbType());
        userRepository.save(entity);
        return UserMapper.toDTO(entity);
    }

//    public Optional<User> findById(String id) {
//        Object typedId = convertId(id);
//        Optional<?> entityOpt = databaseClient.findById(getEntityClass(), typedId);
//        return entityOpt.map(UserMapper::toDTO);
//    }

    public Optional<UserDTO> findById(String id) {
        Object typedId = convertId(id);
        Optional<?> entityOpt = userRepository.findById(typedId);
        return entityOpt.map(UserMapper::toDTO);
    }

    // Convert String id to the appropriate type for the selected backend
    private Object convertId(String id) {
        return switch (getDbType()) {
            case "mysql" -> Integer.parseInt(id);
            case "h2" -> Integer.parseInt(id);
            case "mongodb" -> id; // remains a String
            default -> throw new IllegalStateException("Unsupported db.type: " + getDbType());
        };
    }
}
