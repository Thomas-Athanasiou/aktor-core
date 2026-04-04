package com.aktor.core;

import java.util.Objects;

abstract class SqlStatementParserBase
{
    protected final String table;
    protected final String start;
    protected final String end;

    protected SqlStatementParserBase(final String table, final String start, final String end)
    {
        this.table = Objects.requireNonNull(table);
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
    }

    protected final String quotedTable()
    {
        return start + table + end;
    }
}
