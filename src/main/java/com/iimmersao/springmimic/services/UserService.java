package com.iimmersao.springmimic.services;

import com.iimmersao.springmimic.annotations.Inject;
import com.iimmersao.springmimic.annotations.Service;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.database.DatabaseClient;
import com.iimmersao.springmimic.model.*;
import com.iimmersao.springmimic.web.PageRequest;

import java.util.List;
import java.util.stream.Collectors;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Inject
    private DatabaseClient databaseClient;

    @Inject
    private ConfigLoader config;

    private String getDbType() {
        return config.get("db.type", "mysql").trim().toLowerCase();
    }

    private Class<?> getEntityClass() {
        return switch (getDbType()) {
            case "mysql" -> MySqlUser.class;
            case "h2" -> H2User.class;
            case "mongodb" -> MongoUser.class;
            default -> throw new IllegalStateException("Unsupported db.type: " + getDbType());
        };
    }

    public User save(User dto) {
        Object entity = UserMapper.toEntity(dto, getDbType());
        databaseClient.save(entity);
        return UserMapper.toDTO(entity);
    }

    public Optional<User> findById(String id) {
        Object typedId = convertId(id);
        Optional<?> entityOpt = databaseClient.findById(getEntityClass(), typedId);
        return entityOpt.map(UserMapper::toDTO);
    }

    public List<User> findAll() {
        List<?> entities = databaseClient.findAll(getEntityClass());
        return entities.stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void update(User dto) {
        Object entity = UserMapper.toEntity(dto, getDbType());
        databaseClient.updateById(entity);
    }

    public void deleteById(String id) {
        Object typedId = convertId(id);
        databaseClient.deleteById(getEntityClass(), typedId);
    }

    public void deleteAll() {
        databaseClient.deleteAll(getEntityClass());
    }

    public List<User> findAll(PageRequest pageRequest) {
        List<?> entities = databaseClient.findAll(getEntityClass(), pageRequest);
        return entities.stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
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
