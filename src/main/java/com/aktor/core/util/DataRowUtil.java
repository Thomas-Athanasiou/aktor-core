package com.aktor.core.util;

import com.aktor.core.Row;
import com.aktor.core.Value;
import com.aktor.core.exception.ModelException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DataRowUtil
{
    private DataRowUtil()
    {
    }

    public static Value get(final Row row, final String field)
    {
        final String normalizedField = normalize(field);
        for (final Value entityValue : row.values())
        {
            if (entityValue != null && normalize(entityValue.field()).equals(normalizedField))
            {
                return entityValue;
            }
        }
        return null;
    }

    public static String get(final Map<String, String> fieldMap, final String field)
    {
        if (fieldMap == null || field == null)
        {
            return null;
        }

        final String normalizedField = normalize(field);
        String value = fieldMap.get(normalizedField);
        if (value == null)
        {
            value = fieldMap.get(normalize(SimpleDataObjectConverter.camelToSnake(field)));
        }
        return value;
    }

    public static Value require(final Row row, final String field) throws ModelException
    {
        final Value value = get(row, field);
        if(value == null)
        {
            throw new ModelException(field + " is not set");
        }
        return value;
    }

    public static Row addValue(final Row row, final Value value)
    {
        final Value[] originalValues = row.values();
        final int originalLength = originalValues.length;
        final Value[] values = Arrays.copyOf(originalValues, originalLength + 1);
        values[originalLength] = value;
        return Row.of(values);
    }

    public static Map<String, String> toFieldMap(final Row row)
    {
        final Value[] values = row.values();
        final Map<String, String> fieldMap = new HashMap<>(Math.max(4, values.length * 2));
        for (final Value value : values)
        {
            if (value != null && value.field() != null)
            {
                fieldMap.putIfAbsent(normalize(value.field()), value.value());
            }
        }
        return fieldMap;
    }

    private static String normalize(final String field)
    {
        return field == null ? "" : field.toLowerCase(Locale.ROOT);
    }
}
