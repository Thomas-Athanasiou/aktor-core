package com.aktor.core.model;

import com.aktor.core.Row;
import com.aktor.core.value.Filter;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

final class FilterConditionFieldPredicate
implements FilterCondition
{
    private final BiPredicate<String, String> predicate;

    FilterConditionFieldPredicate(final BiPredicate<String, String> predicate)
    {
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    public boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter)
    {
        final String value = Row.get(fieldMap, filter.field());
        return value != null && predicate.test(value, filter.value());
    }
}
