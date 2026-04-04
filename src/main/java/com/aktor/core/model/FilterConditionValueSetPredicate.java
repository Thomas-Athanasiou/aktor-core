package com.aktor.core.model;

import com.aktor.core.util.DataRowUtil;
import com.aktor.core.value.Filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class FilterConditionValueSetPredicate
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

    FilterConditionValueSetPredicate(final boolean expectedContains)
    {
        this.expectedContains = expectedContains;
    }

    @Override
    public boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter)
    {
        final String value = DataRowUtil.get(fieldMap, filter.field());
        if (filter.value() == null)
        {
            return false;
        }

        return resolveValues(value).contains(filter.value()) == expectedContains;
    }

    private Set<String> resolveValues(final String value)
    {
        final String key = value == null ? "" : value;
        Set<String> values = valuesCache.get(key);
        if (values == null)
        {
            values = value == null || value.isEmpty()
                ? Set.of()
                : new HashSet<>(Arrays.asList(value.split(",")));
            valuesCache.put(key, values);
        }
        return values;
    }
}
