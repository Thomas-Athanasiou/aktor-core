package com.aktor.core.model;

import com.aktor.core.util.CsvValuesUtil;
import com.aktor.core.Row;
import com.aktor.core.value.Filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class FilterConditionMembershipPredicate
implements FilterCondition
{
    private static final int CACHE_SIZE = 256;

    private final boolean expectedContains;
    private final Map<String, Set<String>> valuesCache = Collections.synchronizedMap(new LinkedHashMap<>()
    {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, Set<String>> eldest)
        {
            return size() > CACHE_SIZE;
        }
    });

    FilterConditionMembershipPredicate(final boolean expectedContains)
    {
        this.expectedContains = expectedContains;
    }

    @Override
    public boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter)
    {
        final String value = Row.get(fieldMap, filter.field());
        if (value == null)
        {
            return false;
        }

        return resolveValues(filter.value()).contains(value) == expectedContains;
    }

    private Set<String> resolveValues(final String filterValue)
    {
        final String key = filterValue == null ? "" : filterValue;
        Set<String> values = valuesCache.get(key);
        if (values == null)
        {
            values = new HashSet<>(Arrays.asList(CsvValuesUtil.split(filterValue)));
            valuesCache.put(key, values);
        }
        return values;
    }
}
