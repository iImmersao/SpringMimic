package com.iimmersao.springmimic.database.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcConnectionProvider {
    Connection getConnection() throws SQLException;
    void releaseConnection(Connection connection) throws SQLException;
}
