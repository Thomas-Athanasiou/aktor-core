package com.aktor.core;

import java.util.Arrays;
import java.util.Objects;

public record SearchCriteria(
    FilterGroup[] filterGroups,
    int pageSize,
    int currentPage,
    SortOrder[] sortOrders
)
{
    public SearchCriteria
    {
        filterGroups = Arrays.copyOf(Objects.requireNonNull(filterGroups), filterGroups.length);
        if (pageSize < 1)
        {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (currentPage < 1)
        {
            throw new IllegalArgumentException("currentPage must be greater than 0");
        }
        sortOrders = Arrays.copyOf(Objects.requireNonNull(sortOrders), sortOrders.length);
    }

    @Override
    public FilterGroup[] filterGroups()
    {
        return Arrays.copyOf(filterGroups, filterGroups.length);
    }

    @Override
    public SortOrder[] sortOrders()
    {
        return Arrays.copyOf(sortOrders, sortOrders.length);
    }
}
