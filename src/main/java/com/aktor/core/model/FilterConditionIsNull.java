package com.aktor.core.model;

import com.aktor.core.Row;
import com.aktor.core.value.Filter;

import java.util.Map;

final class FilterConditionIsNull
implements FilterCondition
{
    @Override
    public boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter)
    {
        final String entityValue = Row.get(fieldMap, filter.field());
        return entityValue == null || entityValue.isEmpty();
    }
}
