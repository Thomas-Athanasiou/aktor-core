package com.aktor.core.util;

import com.aktor.core.*;
import com.aktor.core.model.FieldResolver;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;

import java.util.Map;

public final class SqlUtil
{
    private static final String LOGICAL_KEY_FIELD_NAME = "key";

    private SqlUtil()
    {
        super();
    }

    public static <Key> KeySqlSelectParser<Key> ofKeySelectParser(final String tableName, final String driverName)
    {
        return ofKeySelectParser(tableName, driverName, (FieldResolver) null);
    }

    public static <Key> KeySqlSelectParser<Key> ofKeySelectParser(
        final String tableName,
        final String driverName,
        final FieldResolver fieldResolver
    )
    {
        final SqlDialect dialect = ofDialect(driverName);
        return new KeySqlSelectParser<>(
            tableName,
            fieldResolver == null ? LOGICAL_KEY_FIELD_NAME : fieldResolver.resolve(LOGICAL_KEY_FIELD_NAME),
            dialect.quoteStart(),
            dialect.quoteEnd()
        );
    }

    public static <Key> KeySqlSelectParser<Key> ofKeySelectParser(final String tableName, final String keyFieldName, final String driverName)
    {
        final SqlDialect dialect = ofDialect(driverName);
        return new KeySqlSelectParser<>(tableName, keyFieldName, dialect.quoteStart(), dialect.quoteEnd());
    }

    public static <Key> KeySqlDeleteParser<Key> ofKeyDeleteParser(final String tableName, final String keyFieldName, final String driverName)
    {
        final SqlDialect dialect = ofDialect(driverName);
        return new KeySqlDeleteParser<>(tableName, keyFieldName, dialect.quoteStart(), dialect.quoteEnd());
    }

    public static <Key> KeySqlDeleteParser<Key> ofKeyDeleteParser(final String tableName, final String driverName)
    {
        return ofKeyDeleteParser(tableName, LOGICAL_KEY_FIELD_NAME, driverName);
    }

    public static Converter<SearchCriteria, String> ofSearchCriteriaParser(
        final Class<? extends Data<?>> type,
        final String tableName,
        final String keyFieldName,
        final String driverName
    )
    {
        return ofSearchCriteriaParser(tableName, driverName, FieldResolver.mapped(type, Map.of("key", keyFieldName)));
    }

    public static Converter<SearchCriteria, String> ofSearchCriteriaParser(
        final String tableName,
        final String driverName,
        final FieldResolver fieldResolver
    )
    {
        final SqlDialect dialect = ofDialect(driverName);
        return new SearchCriteriaSqlSearchParser(
            tableName,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            dialect,
            fieldResolver
        );
    }

    public static <Item> DataRowSqlUpdateParser<Item> ofDataRowUpdateParser(
        final String tableName,
        final Converter<Item, DataRow> converter,
        final String driverName
    )
    {
        return ofDataRowUpdateParser(tableName, LOGICAL_KEY_FIELD_NAME, converter, driverName);
    }

    public static <Item> DataRowSqlUpdateParser<Item> ofDataRowUpdateParser(
        final String tableName,
        final String keyFieldName,
        final Converter<Item, DataRow> converter,
        final String driverName
    )
    {
        final SqlDialect dialect = ofDialect(driverName);
        return new DataRowSqlUpdateParser<>(
            tableName,
            keyFieldName,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            converter
        );
    }

    public static <Item> DataRowSqlInsertParser<Item> ofDataRowInsertParser(final String tableName, final Converter<Item, DataRow> converter, final String driverName)
    {
        final SqlDialect dialect = ofDialect(driverName);
        return new DataRowSqlInsertParser<>(
            tableName,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            converter
        );
    }

    public static <Item> Converter<Item, String> ofDataRowUpsertParser(
        final String tableName,
        final Converter<Item, DataRow> converter,
        final String driverName
    )
    {
        return ofDataRowUpsertParser(tableName, LOGICAL_KEY_FIELD_NAME, converter, driverName);
    }

    public static <Item> Converter<Item, String> ofDataRowUpsertParser(
        final String tableName,
        final String keyFieldName,
        final Converter<Item, DataRow> converter,
        final String driverName
    )
    {
        final SqlDialect dialect = ofDialect(driverName);
        if (!dialect.supportsUpsert())
        {
            return null;
        }
        return new DataRowSqlUpsertParser<>(tableName, keyFieldName, dialect, converter);
    }

    public static Converter<SearchCriteria, String> ofSearchCriteriaTotalCountParser(
        final Class<? extends Data<?>> type,
        final String tableName,
        final String keyFieldName,
        final String driverName
    )
    {
        return ofSearchCriteriaTotalCountParser(tableName, driverName, FieldResolver.mapped(type, Map.of("key", keyFieldName)));
    }

    public static Converter<SearchCriteria, String> ofSearchCriteriaTotalCountParser(
        final String tableName,
        final String driverName,
        final FieldResolver fieldResolver
    )
    {
        final SqlDialect dialect = ofDialect(driverName);
        return new SearchCriteriaSqlTotalCountParser(
            tableName,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            fieldResolver
        );
    }

    public static Converter<Class<? extends Data<?>>, String> ofClassSchemaParser(final String tableName, final String keyFieldName, final String driverName)
    {
        return type -> ofClassSchemaParser(
            tableName,
            driverName,
            FieldResolver.mapped(type, Map.of(LOGICAL_KEY_FIELD_NAME, keyFieldName))
        ).convert(type);
    }

    public static Converter<Class<? extends Data<?>>, String> ofClassSchemaParser(final String tableName, final String driverName)
    {
        return type -> ofClassSchemaParser(tableName, driverName, FieldResolver.mapped(type)).convert(type);
    }

    public static Converter<Class<? extends Data<?>>, String> ofClassSchemaParser(
        final String tableName,
        final String driverName,
        final FieldResolver fieldResolver
    )
    {
        final SqlDialect dialect = ofDialect(driverName);
        return new ClassSqlSchemaParser(
            tableName,
            fieldResolver.resolve(LOGICAL_KEY_FIELD_NAME),
            dialect.quoteStart(),
            dialect.quoteEnd(),
            fieldResolver
        );
    }

    public static SqlDialect ofDialect(final String driverName)
    {
        return SqlDialectResolver.of(driverName);
    }
}
