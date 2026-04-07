package com.aktor.core.model;

public final class SqlDialectAnsi
extends SqlDialectAbstract
{
    @Override
    public String name()
    {
        return "ansi";
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
        return false;
    }

    @Override
    public String upsertClause(final String keyField, final String[] fieldNames)
    {
        throw new UnsupportedOperationException("ANSI dialect upsert is not supported");
    }
}

