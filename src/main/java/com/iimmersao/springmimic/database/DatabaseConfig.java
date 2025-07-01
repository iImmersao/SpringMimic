package com.iimmersao.springmimic.database;

import com.iimmersao.springmimic.core.ConfigLoader;

public class DatabaseConfig {
    private final String type;
    private final String host;
    private final int port;
    private final String dbName;
    private final String username;
    private final String password;
    private final String uri; // optional for Mongo

    public DatabaseConfig() {
        this.type = ConfigLoader.get("db.type", "mysql").toLowerCase();
        this.host = ConfigLoader.get("db.host", "localhost");
        this.port = ConfigLoader.getInt("db.port", this.type.equals("mongodb") ? 27017 : 3306);
        this.dbName = ConfigLoader.get("db.name", "test");
        this.username = ConfigLoader.get("db.username", "");
        this.password = ConfigLoader.get("db.password", "");
        this.uri = ConfigLoader.get("db.uri"); // optional
    }

    public String getType() { return type; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDbName() { return dbName; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getUri() { return uri; }

    public boolean isMongo() { return "mongodb".equals(type); }
    public boolean isMySql() { return "mysql".equals(type); }
}
