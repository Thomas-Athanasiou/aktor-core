package com.aktor.core.model;

import com.aktor.core.Row;
import com.aktor.core.value.Filter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class FilterConditionPatternPredicate
implements FilterCondition
{
    private static final int CACHE_SIZE = 256;
    private static final Pattern META_CHARACTERS = Pattern.compile("([\\\\+\\-\\[\\](){}.*?^$|])");

    private final boolean expectedMatch;
    private final Map<String, Pattern> patternCache = Collections.synchronizedMap(new LinkedHashMap<>()
    {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, Pattern> eldest)
        {
            return size() > CACHE_SIZE;
        }
    });

    FilterConditionPatternPredicate(final boolean expectedMatch)
    {
        this.expectedMatch = expectedMatch;
    }

    @Override
    public boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter)
    {
        final String filterValue = filter.value();
        final String entityValue = Row.get(fieldMap, filter.field());
        if (entityValue == null || filterValue == null)
        {
            return false;
        }

        return resolvePattern(filterValue).matcher(entityValue).matches() == expectedMatch;
    }

    private Pattern resolvePattern(final String filterValue)
    {
        Pattern pattern = patternCache.get(filterValue);
        if (pattern == null)
        {
            final String expression = META_CHARACTERS.matcher(filterValue).replaceAll("\\\\$1")
                .replace("\\%", "%").replace("\\_", "_")
                .replace("%", ".*").replace("_", ".");
            pattern = Pattern.compile("(?i)" + expression);
            patternCache.put(filterValue, pattern);
        }
        return pattern;
    }
}
