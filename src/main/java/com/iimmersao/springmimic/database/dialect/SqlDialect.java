package com.iimmersao.springmimic.database.dialect;

public interface SqlDialect {
    String name();

    default boolean useColumnAnnotations() {
        return true;
    }

    default boolean supportsLimitOffset() {
        return true;
    }

    default boolean supportsLike() {
        return true;
    }

    default boolean supportsSelectLiteral() {
        return true;
    }

    default boolean invalidFindByIdReturnsEmpty() {
        return false;
    }

    default String limitOffsetClause() {
        return " LIMIT ? OFFSET ?";
    }

    default String existsSelectExpression(String columnName) {
        return supportsSelectLiteral() ? "1" : columnName;
    }
}
