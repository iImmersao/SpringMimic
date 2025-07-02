package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.annotations.DocumentType;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import com.mongodb.client.*;
import org.bson.BsonDocument;

import java.util.List;
import java.util.Optional;

public class MongoDatabaseClient implements DatabaseClient {

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;

    public MongoDatabaseClient() {
        try {
            String uri = ConfigLoader.get("mongodb.uri");
            String dbName = ConfigLoader.get("mongodb.database");

            mongoClient = MongoClients.create(uri);
            mongoDatabase = mongoClient.getDatabase(dbName);
        } catch (Exception e) {
            throw new DatabaseException("Failed to initialize MongoDB client", e);
        }
    }

    private MongoCollection<BsonDocument> getCollection(Class<?> type) {
        if (!type.isAnnotationPresent(DocumentType.class)) {
            throw new DatabaseException("Missing @Document annotation on " + type.getName());
        }
        String collectionName = type.getAnnotation(DocumentType.class).collection();
        return mongoDatabase.getCollection(collectionName, BsonDocument.class);
    }

    // Placeholder implementations
    @Override
    public <T> void save(T entity) {
        throw new UnsupportedOperationException("Mongo save() not yet implemented");
    }

    @Override
    public <T> Optional<T> findById(Class<T> type, Object id) {
        throw new UnsupportedOperationException("Mongo findById() not yet implemented");
    }

    @Override
    public <T> List<T> findAll(Class<T> type) {
        throw new UnsupportedOperationException("Mongo findAll() not yet implemented");
    }
    
    @Override
    public <T> void updateById(T entity) {
        throw new UnsupportedOperationException("Mongo updateById() not yet implemented");
    }

    @Override
    public <T> void deleteById(Class<T> entityType, Object id) {
        throw new UnsupportedOperationException("Mongo deleteById() not yet implemented");
    }

    @Override
    public <T> void deleteAll(Class<T> type) {
        throw new UnsupportedOperationException("Mongo deleteAll() not yet implemented");
    }
}
