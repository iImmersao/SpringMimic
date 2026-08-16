package com.iimmersao.springmimic.database.dialect;

public class H2Dialect implements SqlDialect {
    @Override
    public String name() {
        return "H2";
    }

    @Override
    public boolean useColumnAnnotations() {
        return false;
    }

    @Override
    public boolean invalidFindByIdReturnsEmpty() {
        return true;
    }
}
