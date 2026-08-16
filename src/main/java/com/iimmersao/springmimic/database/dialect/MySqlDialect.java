package com.iimmersao.springmimic.database.dialect;

public class MySqlDialect implements SqlDialect {
    @Override
    public String name() {
        return "MySQL";
    }
}
