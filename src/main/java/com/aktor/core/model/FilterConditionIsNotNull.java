package com.aktor.core.model;

import com.aktor.core.util.DataRowUtil;
import com.aktor.core.value.Filter;

import java.util.Map;

final class FilterConditionIsNotNull
implements FilterCondition
{
    @Override
    public boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter)
    {
        final String entityValue = DataRowUtil.get(fieldMap, filter.field());
        return entityValue != null && !entityValue.isEmpty();
    }
}
