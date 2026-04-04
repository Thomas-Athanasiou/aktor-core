package com.aktor.core.util;

import com.aktor.core.DataRow;
import com.aktor.core.Value;
import com.aktor.core.exception.ModelException;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DataRowUtil
{
    private DataRowUtil()
    {
    }

    public static Value get(final DataRow dataRow, final String field)
    {
        final String normalizedField = normalize(field);
        for (final Value entityValue : dataRow.values())
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

    public static Value require(final DataRow dataRow, final String field) throws ModelException
    {
        final Value value = get(dataRow, field);
        if(value == null)
        {
            throw new ModelException(field + " is not set");
        }
        return value;
    }

    public static DataRow addValue(final DataRow dataRow, final Value value)
    {
        final Value[] originalValues = dataRow.values();
        final int originalLength = originalValues.length;
        final Value[] values = Arrays.copyOf(originalValues, originalLength + 1);
        values[originalLength] = value;
        return DataRow.of(values);
    }

    public static Map<String, String> toFieldMap(final DataRow dataRow)
    {
        final Value[] values = dataRow.values();
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
