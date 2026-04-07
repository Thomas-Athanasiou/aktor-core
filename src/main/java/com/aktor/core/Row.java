package com.aktor.core;

import java.util.Arrays;
import java.util.Collection;
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
}
