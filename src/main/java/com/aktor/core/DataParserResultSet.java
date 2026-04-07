package com.aktor.core;

import com.aktor.core.exception.ConversionException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataParserResultSet<Item extends java.lang.Record & Data<?>>
implements Converter<ResultSet, Item>
{
    private final Mapper<Item, ?> mapper;

    private volatile ColumnLayout cachedLayout = null;

    public DataParserResultSet(final Mapper<Item, ?> mapper)
    {
        super();
        this.mapper = Objects.requireNonNull(mapper);
    }

    public Item convert(final ResultSet resultSet) throws ConversionException
    {
        try
        {
            final String[] columnKeys = resolveColumnKeys(resultSet);
            final Map<String, String> map = new HashMap<>(Math.max(4, columnKeys.length * 2));
            for (int position = 1; position <= columnKeys.length; position++)
            {
                map.put(columnKeys[position - 1], resultSet.getString(position));
            }
            return mapper.convert(map);
        }
        catch (final SQLException exception)
        {
            throw new ConversionException(exception);
        }
    }

    private String[] resolveColumnKeys(final ResultSet resultSet) throws SQLException
    {
        final ColumnLayout layout = cachedLayout;
        if (layout != null && layout.resultSet() == resultSet)
        {
            return layout.columnKeys();
        }

        final ResultSetMetaData meta = resultSet.getMetaData();
        final int count = meta.getColumnCount();
        final String[] columnKeys = new String[count];
        final Map<String, Boolean> seen = new HashMap<>(Math.max(4, count * 2));
        for (int position = 1; position <= count; position++)
        {
            final String label = resolveLabel(meta, position);
            String key = label;
            if (seen.putIfAbsent(key, Boolean.TRUE) != null)
            {
                final String tableName = meta.getTableName(position);
                final String qualified = tableName == null || tableName.isBlank() ? null : tableName + "." + label;
                if (qualified != null && seen.putIfAbsent(qualified, Boolean.TRUE) == null)
                {
                    key = qualified;
                }
                else
                {
                    key = nextDuplicateKey(seen, label);
                }
            }
            columnKeys[position - 1] = key;
        }
        cachedLayout = new ColumnLayout(resultSet, columnKeys);
        return columnKeys;
    }

    private static String resolveLabel(final ResultSetMetaData meta, final int position) throws SQLException
    {
        final String label = meta.getColumnLabel(position);
        if (label == null || label.isBlank())
        {
            final String fallback = meta.getColumnName(position);
            return fallback == null || fallback.isBlank() ? "column_" + position : fallback;
        }
        return label;
    }

    private static String nextDuplicateKey(final Map<String, ?> map, final String label)
    {
        int suffix = 2;
        String key = label + "#" + suffix;
        while (map.containsKey(key))
        {
            key = label + "#" + ++suffix;
        }
        return key;
    }

    record ColumnLayout(ResultSet resultSet, String[] columnKeys)
    {
    }
}
