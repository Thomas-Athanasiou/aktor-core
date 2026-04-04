package com.aktor.core.util;

import com.aktor.core.FilterGroup;
import com.aktor.core.value.Filter;

import java.util.Arrays;

public final class FilterGroupUtil
{
    private FilterGroupUtil()
    {
    }

    public static FilterGroup addFilter(final FilterGroup filterGroup, final Filter filter)
    {
        final int originalLength = filterGroup.filters().length;
        final Filter[] filters = Arrays.copyOf(filterGroup.filters(), originalLength + 1);
        filters[originalLength] = filter;
        return new FilterGroup(filters);
    }
}
