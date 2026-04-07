package com.aktor.core.model;

public final class SqlDialectSqlite
extends SqlDialectAbstract
{
    @Override
    public String name()
    {
        return "sqlite";
    }

    @Override
    public String quoteStart()
    {
        return "\"";
    }

    @Override
    public String quoteEnd()
    {
        return "\"";
    }

    @Override
    public boolean supportsUpsert()
    {
        return true;
    }

    @Override
    public String upsertClause(final String keyField, final String[] fieldNames)
    {
        final String key = quoteField(keyField);
        final String assignments = toConflictAssignments(
            keyField,
            fieldNames,
            fieldName -> quoteField(fieldName) + " = excluded." + quoteField(fieldName)
        );
        return assignments.isEmpty()
            ? "ON CONFLICT(" + key + ") DO NOTHING"
            : "ON CONFLICT(" + key + ") DO UPDATE SET " + assignments;
    }
}

