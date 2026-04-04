package com.aktor.core;

import com.aktor.core.value.Filter;

import java.util.Arrays;
import java.util.Objects;

public record FilterGroup(Filter[] filters)
{
    public FilterGroup
    {
        if (Objects.requireNonNull(filters).length < 1)
        {
            throw new IllegalArgumentException("filters cannot be empty");
        }
        filters = Arrays.copyOf(filters, filters.length);
    }

    @Override
    public Filter[] filters()
    {
        return Arrays.copyOf(filters, filters.length);
    }
}
