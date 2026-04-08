package com.aktor.core;

import com.aktor.core.exception.ModelException;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record Row(Value[] values)
{
    private static final Value[] VALUES = new Value[0];

    public Row
    {
        values = Arrays.copyOf(Objects.requireNonNull(values), values.length);
    }

    @Override
    public Value[] values()
    {
        return Arrays.copyOf(values, values.length);
    }

    public static Row empty()
    {
        return new Row(VALUES);
    }

    public static Row of(final Value... values)
    {
        return new Row(Objects.requireNonNull(values));
    }

    public static Row of(final Collection<Value> values)
    {
        return new Row(Objects.requireNonNull(values).toArray(VALUES));
    }

    public static Value get(final Row row, final String field)
    {
        final Row safeRow = Objects.requireNonNull(row);
        final String normalizedField = normalize(field);
        for (final Value entityValue : safeRow.values())
        {
            if (entityValue != null && normalize(entityValue.field()).equals(normalizedField))
            {
                return entityValue;
            }
        }
        return null;
    }

    public static Value require(final Row row, final String field) throws ModelException
    {
        final Value value = get(row, field);
        if (value == null)
        {
            throw new ModelException(field + " is not set");
        }
        return value;
    }

    public static Row addValue(final Row row, final Value value)
    {
        final Row safeRow = Objects.requireNonNull(row);
        final Value safeValue = Objects.requireNonNull(value);
        final Value[] values = safeRow.values();
        final Value[] next = Arrays.copyOf(values, values.length + 1);
        next[values.length] = safeValue;
        return Row.of(next);
    }

    public static Map<String, String> toFieldMap(final Row row)
    {
        final Row safeRow = Objects.requireNonNull(row);
        final Value[] values = safeRow.values();
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

    private static String normalize(final String field)
    {
        return field == null ? "" : field.toLowerCase(Locale.ROOT);
    }
}
