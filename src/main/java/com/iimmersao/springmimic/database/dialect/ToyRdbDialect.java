package com.iimmersao.springmimic.database.dialect;

public class ToyRdbDialect implements SqlDialect {
    @Override
    public String name() {
        return "ToyRDB";
    }

    @Override
    public boolean supportsLimitOffset() {
        return false;
    }

    @Override
    public boolean supportsLike() {
        return false;
    }

    @Override
    public boolean supportsSelectLiteral() {
        return false;
    }
}
