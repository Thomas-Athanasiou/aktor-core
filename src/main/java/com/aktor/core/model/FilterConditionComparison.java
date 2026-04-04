package com.aktor.core.model;

import com.aktor.core.util.DataRowUtil;
import com.aktor.core.value.Filter;

import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;

final class FilterConditionComparison
implements FilterCondition
{
    private final IntPredicate predicate;

    FilterConditionComparison(final IntPredicate predicate)
    {
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    public boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter)
    {
        final String value = DataRowUtil.get(fieldMap, filter.field());
        return value != null && filter.value() != null && predicate.test(compare(value, filter.value()));
    }

    private static int compare(final String left, final String right)
    {
        int comparison;
        try
        {
            comparison = Double.compare(Double.parseDouble(left), Double.parseDouble(right));
        }
        catch (final NumberFormatException exception)
        {
            comparison = left.compareTo(right);
        }
        return comparison;
    }
}
