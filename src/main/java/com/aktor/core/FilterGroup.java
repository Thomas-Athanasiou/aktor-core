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

    public static FilterGroup addFilter(final FilterGroup filterGroup, final Filter filter)
    {
        final FilterGroup safeFilterGroup = Objects.requireNonNull(filterGroup);
        final Filter safeFilter = Objects.requireNonNull(filter);
        final Filter[] filters = safeFilterGroup.filters();
        final Filter[] next = Arrays.copyOf(filters, filters.length + 1);
        next[filters.length] = safeFilter;
        return new FilterGroup(next);
    }
}
