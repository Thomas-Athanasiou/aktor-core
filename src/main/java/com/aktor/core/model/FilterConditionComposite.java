package com.aktor.core.model;

import com.aktor.core.ConditionType;
import com.aktor.core.value.Filter;

import java.util.Map;
import java.util.Objects;

final class FilterConditionComposite
implements FilterCondition
{
    private final Map<ConditionType, FilterCondition> map;

    public FilterConditionComposite(final Map<ConditionType, FilterCondition> map)
    {
        super();
        this.map = Objects.requireNonNull(map);
    }

    @Override
    public boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter)
    {
        final FilterCondition condition = map.get(filter.conditionType());
        return condition != null && condition.isEntityMatch(fieldMap, filter);
    }
}
