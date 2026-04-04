package com.aktor.core.model;

import com.aktor.core.FilterGroup;
import com.aktor.core.Model;

import java.util.Map;
import java.util.Objects;

final class FilterGroupCondition
implements Model
{
    private final FilterCondition filterCondition;

    public FilterGroupCondition(final FilterCondition filterCondition)
    {
        super();
        this.filterCondition = Objects.requireNonNull(filterCondition);
    }

    public boolean isEntityMatch(final Map<String, String> fieldMap, final FilterGroup filterGroup)
    {
        for (final var filter : filterGroup.filters())
        {
            if (filterCondition.isEntityMatch(fieldMap, filter))
            {
                return true;
            }
        }
        return false;
    }
}
