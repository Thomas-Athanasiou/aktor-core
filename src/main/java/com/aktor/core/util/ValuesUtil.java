package com.aktor.core.util;

import com.aktor.core.Value;
import com.aktor.core.exception.ModelException;

import java.util.Arrays;

public final class ValuesUtil
{
    private ValuesUtil()
    {
        super();
    }

    public static Value get(final Value[] values, final String field)
    {
        return Arrays.stream(values)
            .filter(value -> value.field().equalsIgnoreCase(field))
            .findFirst()
            .orElse(null);
    }

    public static Value require(final Value[] values, final String field) throws ModelException
    {
        final Value value = ValuesUtil.get(values, field);
        if(value == null)
        {
            throw new ModelException(field + " is not set");
        }
        return value;
    }

    public static Value[] addValue(final Value[] values, final Value value)
    {
        final int originalLength = values.length;
        final Value[] newValues = Arrays.copyOf(values, originalLength + 1);
        newValues[originalLength] = value;
        return newValues;
    }
}
