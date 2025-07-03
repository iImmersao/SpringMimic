package com.iimmersao.springmimic.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iimmersao.springmimic.annotations.Bean;
import com.iimmersao.springmimic.annotations.Column;
import com.iimmersao.springmimic.annotations.Id;
import com.iimmersao.springmimic.annotations.Table;
import com.iimmersao.springmimic.core.ConfigLoader;
import com.iimmersao.springmimic.exceptions.DatabaseException;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.*;

import java.lang.reflect.Field;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

@Bean
public class MongoDatabaseClient implements DatabaseClient {

    private ConfigLoader config;
    private final MongoClient client;
    private final MongoDatabase database;
    private final ObjectMapper objectMapper;

    public MongoDatabaseClient(ConfigLoader config) {
        this.config = config;
        try {
            String uri = config.get("mongodb.uri");
            String dbName = config.get("mongodb.database");
            this.client = MongoClients.create(uri);
            this.database = client.getDatabase(dbName);
            this.objectMapper = new ObjectMapper();
        } catch (Exception e) {
            throw new DatabaseException("Failed to connect to MongoDB", e);
        }
    }

    @Override
    public <T> void save(T entity) {
        try {
            String collectionName = getCollectionName(entity.getClass());
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document doc = toBsonDocument(entity);
            collection.insertOne(doc);

            // Set generated ID back to entity
            ObjectId objectId = doc.getObjectId("_id");
            Field idField = getIdField(entity.getClass());
            idField.setAccessible(true);
            if (idField.getType() == String.class) {
                idField.set(entity, objectId.toHexString());
            } else {
                idField.set(entity, objectId);
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to save entity", e);
        }
    }

    @Override
    public <T> Optional<T> findById(Class<T> entityType, Object id) {
        try {
            String collectionName = getCollectionName(entityType);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document doc = collection.find(eq("_id", convertToObjectId(id))).first();
            if (doc == null) return Optional.empty();

            return Optional.of(fromDocument(doc, entityType));
        } catch (Exception e) {
            throw new DatabaseException("Failed to find entity by ID", e);
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityType) {
        try {
            String collectionName = getCollectionName(entityType);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<T> results = new ArrayList<>();
            for (Document doc : collection.find()) {
                results.add(fromDocument(doc, entityType));
            }
            return results;
        } catch (Exception e) {
            throw new DatabaseException("Failed to retrieve all entities", e);
        }
    }

    @Override
    public <T> void updateById(T entity) {
        try {
            Class<?> clazz = entity.getClass();
            String collectionName = getCollectionName(clazz);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Field idField = getIdField(clazz);
            idField.setAccessible(true);
            Object idValue = idField.get(entity);
            ObjectId objectId = convertToObjectId(idValue);

            Document updatedDoc = toBsonDocument(entity);
            collection.replaceOne(eq("_id", objectId), updatedDoc);
        } catch (Exception e) {
            throw new DatabaseException("Failed to update entity by ID", e);
        }
    }

    @Override
    public <T> void deleteById(Class<T> entityType, Object id) {
        try {
            String collectionName = getCollectionName(entityType);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            collection.deleteOne(eq("_id", convertToObjectId(id)));
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete entity by ID", e);
        }
    }

    @Override
    public <T> void deleteAll(Class<T> entityType) {
        try {
            String collectionName = getCollectionName(entityType);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            collection.deleteMany(new Document());
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete all entities", e);
        }
    }

    // ----------------------
    // Utility Methods
    // ----------------------

    private String getCollectionName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return clazz.getSimpleName().toLowerCase();
    }

    private Document toBsonDocument(Object entity) throws IllegalAccessException {
        Document doc = new Document();
        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String name = getColumnName(field);
            Object value = field.get(entity);
            if (field.isAnnotationPresent(Id.class)) {
                if (value != null) {
                    doc.put("_id", convertToObjectId(value));
                }
            } else {
                doc.put(name, value);
            }
        }
        return doc;
    }

    private <T> T fromDocument(Document doc, Class<T> clazz) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String name = getColumnName(field);
            Object value;
            if (field.isAnnotationPresent(Id.class)) {
                ObjectId objectId = doc.getObjectId("_id");
                if (field.getType() == String.class) {
                    value = objectId.toHexString();
                } else {
                    value = objectId;
                }
            } else {
                value = doc.get(name);
            }
            field.set(instance, value);
        }
        return instance;
    }

    private Field getIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new IllegalStateException("No field annotated with @Id in class " + clazz.getName());
    }

    private String getColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        return (column != null && !column.name().isEmpty()) ? column.name() : field.getName();
    }

    private ObjectId convertToObjectId(Object id) {
        if (id instanceof ObjectId) {
            return (ObjectId) id;
        } else if (id instanceof String) {
            return new ObjectId((String) id);
        } else {
            throw new IllegalArgumentException("Unsupported ID type: " + id.getClass());
        }
    }
}
