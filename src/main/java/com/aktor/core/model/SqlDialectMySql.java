package com.aktor.core.model;

public final class SqlDialectMySql
extends SqlDialectAbstract
{
    @Override
    public String name()
    {
        return "mysql";
    }

    @Override
    public String quoteStart()
    {
        return "`";
    }

    @Override
    public String quoteEnd()
    {
        return "`";
    }

    @Override
    public boolean supportsUpsert()
    {
        return true;
    }

    @Override
    public String upsertClause(final String keyField, final String[] fieldNames)
    {
        final String alias = "new_row";
        final String assignments = toConflictAssignments(
            keyField,
            fieldNames,
            fieldName -> quoteField(fieldName) + " = " + alias + "." + quoteField(fieldName)
        );
        if (!assignments.isEmpty())
        {
            return "AS " + alias + " ON DUPLICATE KEY UPDATE " + assignments;
        }
        final String key = quoteField(keyField);
        return "AS " + alias + " ON DUPLICATE KEY UPDATE " + key + " = " + alias + "." + key;
    }
}
