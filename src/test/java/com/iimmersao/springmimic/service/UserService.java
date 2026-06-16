package com.iimmersao.springmimic.service;

import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.annotations.Service;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.model.BaseUserEntity;
import com.iimmersao.springmimic.model.H2UserEntity;
import com.iimmersao.springmimic.model.MySqlUserEntity;
import com.iimmersao.springmimic.model.MongoUserEntity;
import com.iimmersao.springmimic.model.UserDTO;
import com.iimmersao.springmimic.model.UserMapper;
import com.iimmersao.springmimic.testcomponents.H2UserRepository;
import com.iimmersao.springmimic.testcomponents.MongoUserRepository;
import com.iimmersao.springmimic.testcomponents.MySqlUserRepository;
import com.iimmersao.springmimic.testcomponents.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository<?, ?> userRepository;

    private final ConfigLoader config;

    private final String dbType;

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

    public UserDTO save(UserDTO dto) {
        BaseUserEntity<?> entity = UserMapper.toEntity(dto, dbType);
        saveEntity(entity);
        return UserMapper.toDTO(entity);
    }

    public Optional<UserDTO> findById(String id) {
        Object typedId = convertId(id);
        Optional<? extends BaseUserEntity<?>> entityOpt = findEntityById(typedId);
        return entityOpt.map(UserMapper::toDTO);
    }

    // Convert String id to the appropriate type for the selected backend
    private Object convertId(String id) {
        return switch (dbType) {
            case "mysql", "h2" -> Integer.parseInt(id);
            case "mongodb" -> id; // remains a String
            default -> throw new IllegalStateException("Unsupported db.type: " + dbType);
        };
    }

    private void saveEntity(BaseUserEntity<?> entity) {
        if (userRepository instanceof MySqlUserRepository repository && entity instanceof MySqlUserEntity user) {
            repository.save(user);
            return;
        }
        if (userRepository instanceof H2UserRepository repository && entity instanceof H2UserEntity user) {
            repository.save(user);
            return;
        }
        if (userRepository instanceof MongoUserRepository repository && entity instanceof MongoUserEntity user) {
            repository.save(user);
            return;
        }
        throw new IllegalStateException("Repository and entity type do not match db.type: " + dbType);
    }

    private Optional<? extends BaseUserEntity<?>> findEntityById(Object typedId) {
        if (userRepository instanceof MySqlUserRepository repository && typedId instanceof Integer id) {
            return repository.findById(id);
        }
        if (userRepository instanceof H2UserRepository repository && typedId instanceof Integer id) {
            return repository.findById(id);
        }
        if (userRepository instanceof MongoUserRepository repository && typedId instanceof String id) {
            return repository.findById(id);
        }
        throw new IllegalStateException("Repository and id type do not match db.type: " + dbType);
    }
}
