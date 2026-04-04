package com.aktor.core.util.value;

import com.aktor.core.value.Filter;

import java.util.Arrays;

public final class FiltersUtil
{
    private FiltersUtil()
    {
        super();
    }

    public static Filter[] addFilter(final Filter[] filters, final Filter filter)
    {
        final int originalLength = filters.length;
        final Filter[] result = Arrays.copyOf(filters, originalLength + 1);
        result[originalLength] = filter;
        return result;
    }
}
