package com.aktor.core.model;

import java.util.Locale;
import java.util.Map;

public final class SqlDialectResolver
{
    private static final Map<String, SqlDialect> DIALECTS = Map.ofEntries(
        Map.entry("mysql", new SqlDialectMySql()),
        Map.entry("postgresql", new SqlDialectPostgresql()),
        Map.entry("sqlserver", new SqlDialectSqlServer()),
        Map.entry("ansi", new SqlDialectAnsi()),
        Map.entry("sqlite", new SqlDialectSqlite())
    );

    private SqlDialectResolver()
    {
        super();
    }

    public static SqlDialect of(final String driverName)
    {
        if (driverName == null || driverName.isBlank())
        {
            throw new IllegalArgumentException("driverName cannot be null or blank");
        }

        final String driverNameLower = driverName.toLowerCase(Locale.ROOT);
        final SqlDialect dialect = DIALECTS.get(driverNameLower);
        if (dialect == null)
        {
            throw new IllegalArgumentException("Unsupported SQL driver '" + driverName + "'");
        }
        return dialect;
    }
}

