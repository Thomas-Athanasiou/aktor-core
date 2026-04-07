package com.aktor.core.util;

import com.aktor.core.*;
import com.aktor.core.model.FieldNormalizer;
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

    public static <Key> KeySqlSelectParser<Key> ofKeySelectParser(final String table, final String driver)
    {
        return ofKeySelectParser(table, driver, (FieldNormalizer) null);
    }

    public static <Key> KeySqlSelectParser<Key> ofKeySelectParser(
        final String table,
        final String driver,
        final FieldNormalizer fieldResolver
    )
    {
        final SqlDialect dialect = ofDialect(driver);
        return new KeySqlSelectParser<>(
            table,
            fieldResolver == null ? LOGICAL_KEY_FIELD_NAME : fieldResolver.resolve(LOGICAL_KEY_FIELD_NAME),
            dialect.quoteStart(),
            dialect.quoteEnd()
        );
    }


    // TODO IS THIS METHOD STILL NEEDED TO BE PUBLIC?
    public static <Key> KeySqlSelectParser<Key> ofKeySelectParser(final String table, final String keyField, final String driver)
    {
        final SqlDialect dialect = ofDialect(driver);
        return new KeySqlSelectParser<>(table, keyField, dialect.quoteStart(), dialect.quoteEnd());
    }

    // TODO IS THIS METHOD STILL NEEDED TO BE PUBLIC?
    public static <Key> KeySqlDeleteParser<Key> ofKeyDeleteParser(final String table, final String keyField, final String driver)
    {
        final SqlDialect dialect = ofDialect(driver);
        return new KeySqlDeleteParser<>(table, keyField, dialect.quoteStart(), dialect.quoteEnd());
    }

    public static <Key> KeySqlDeleteParser<Key> ofKeyDeleteParser(final String table, final String driver)
    {
        return ofKeyDeleteParser(table, LOGICAL_KEY_FIELD_NAME, driver);
    }

    public static Converter<SearchCriteria, String> ofSearchCriteriaParser(
        final Class<? extends Data<?>> type,
        final String table,
        final String keyField,
        final String driver
    )
    {
        return ofSearchCriteriaParser(table, driver, FieldResolver.mapped(type, Map.of("key", keyField)));
    }

    public static Converter<SearchCriteria, String> ofSearchCriteriaParser(
        final String table,
        final String driver,
        final FieldNormalizer fieldResolver
    )
    {
        final SqlDialect dialect = ofDialect(driver);
        return new SearchCriteriaSqlSearchParser(
            table,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            dialect,
            fieldResolver
        );
    }

    public static <Item> DataRowSqlUpdateParser<Item> ofDataRowUpdateParser(
        final String table,
        final Converter<Item, DataRow> converter,
        final String driver
    )
    {
        return ofDataRowUpdateParser(table, LOGICAL_KEY_FIELD_NAME, converter, driver);
    }

    public static <Item> DataRowSqlUpdateParser<Item> ofDataRowUpdateParser(
        final String table,
        final String keyField,
        final Converter<Item, DataRow> converter,
        final String driver
    )
    {
        final SqlDialect dialect = ofDialect(driver);
        return new DataRowSqlUpdateParser<>(
            table,
            keyField,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            converter
        );
    }

    public static <Item> DataRowSqlInsertParser<Item> ofDataRowInsertParser(final String table, final Converter<Item, DataRow> converter, final String driver)
    {
        final SqlDialect dialect = ofDialect(driver);
        return new DataRowSqlInsertParser<>(
            table,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            converter
        );
    }

    public static <Item> Converter<Item, String> ofDataRowUpsertParser(
        final String table,
        final Converter<Item, DataRow> converter,
        final String driver
    )
    {
        return ofDataRowUpsertParser(table, LOGICAL_KEY_FIELD_NAME, converter, driver);
    }

    public static <Item> Converter<Item, String> ofDataRowUpsertParser(
        final String table,
        final String keyField,
        final Converter<Item, DataRow> converter,
        final String driver
    )
    {
        final SqlDialect dialect = ofDialect(driver);
        if (!dialect.supportsUpsert())
        {
            return null;
        }
        return new DataRowSqlUpsertParser<>(table, keyField, dialect, converter);
    }

    public static Converter<SearchCriteria, String> ofSearchCriteriaTotalCountParser(
        final Class<? extends Data<?>> type,
        final String table,
        final String keyField,
        final String driver
    )
    {
        return ofSearchCriteriaTotalCountParser(table, driver, FieldResolver.mapped(type, Map.of("key", keyField)));
    }

    public static Converter<SearchCriteria, String> ofSearchCriteriaTotalCountParser(
        final String table,
        final String driver,
        final FieldNormalizer fieldResolver
    )
    {
        final SqlDialect dialect = ofDialect(driver);
        return new SearchCriteriaSqlTotalCountParser(
            table,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            fieldResolver
        );
    }

    public static Converter<Class<? extends Data<?>>, String> ofClassSchemaParser(
        final String table,
        final String keyField,
        final String driver
    )
    {
        return type -> ofClassSchemaParser(
            table,
            driver,
            FieldResolver.mapped(type, Map.of(LOGICAL_KEY_FIELD_NAME, keyField))
        ).convert(type);
    }

    public static Converter<Class<? extends Data<?>>, String> ofClassSchemaParser(final String table, final String driver)
    {
        return type -> ofClassSchemaParser(table, driver, FieldResolver.mapped(type)).convert(type);
    }

    public static Converter<Class<? extends Data<?>>, String> ofClassSchemaParser(
        final String table,
        final String driver,
        final FieldNormalizer fieldResolver
    )
    {
        final SqlDialect dialect = ofDialect(driver);
        return new ClassSqlSchemaParser(
            table,
            fieldResolver.resolve(LOGICAL_KEY_FIELD_NAME),
            dialect.quoteStart(),
            dialect.quoteEnd(),
            fieldResolver
        );
    }

    public static SqlDialect ofDialect(final String driver)
    {
        return SqlDialectResolver.of(driver);
    }
}
