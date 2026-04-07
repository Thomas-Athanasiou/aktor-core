package com.aktor.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public record DataRow(Value[] values)
{
    private static final Value[] VALUES = new Value[0];

    public DataRow
    {
        values = Arrays.copyOf(Objects.requireNonNull(values), values.length);
    }

    @Override
    public Value[] values()
    {
        return Arrays.copyOf(values, values.length);
    }

    public static DataRow empty()
    {
        return new DataRow(VALUES);
    }

    public static DataRow of(final Value... values)
    {
        return new DataRow(Objects.requireNonNull(values));
    }

    public static DataRow of(final Collection<Value> values)
    {
        return new DataRow(Objects.requireNonNull(values).toArray(VALUES));
    }

    public static DataRow single(final String field, final String value)
    {
        return new DataRow(new Value[] {new Value(field, value)});
    }
}

