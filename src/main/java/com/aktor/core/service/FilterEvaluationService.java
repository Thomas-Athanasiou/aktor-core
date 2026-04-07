package com.aktor.core.service;

import com.aktor.core.Row;
import com.aktor.core.FilterGroup;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SortOrder;
import com.aktor.core.model.SearchCriteriaCondition;
import com.aktor.core.value.Filter;

import java.util.Map;
import java.util.Objects;

// TODO Fewer isMatch()
public final class FilterEvaluationService
{
    private static final SortOrder[] SORT_ORDERS = new SortOrder[0];

    private final SearchCriteriaCondition searchCriteriaCondition;

    public FilterEvaluationService()
    {
        this(new SearchCriteriaCondition());
    }

    public FilterEvaluationService(final SearchCriteriaCondition searchCriteriaCondition)
    {
        this.searchCriteriaCondition = Objects.requireNonNull(searchCriteriaCondition);
    }

    public boolean isMatch(final Row row, final Filter filter)
    {
        return isMatch(Objects.requireNonNull(row), allOf(Objects.requireNonNull(filter)));
    }

    public boolean isMatch(
        final Map<String, String> fieldMap,
        final Filter filter
    )
    {
        return isMatch(Objects.requireNonNull(fieldMap), allOf(Objects.requireNonNull(filter)));
    }

    public boolean isMatch(
        final Row row,
        final FilterGroup filterGroup
    )
    {
        return isMatch(Objects.requireNonNull(row), criteria(Objects.requireNonNull(filterGroup)));
    }

    public boolean isMatch(
        final Map<String, String> fieldMap,
        final FilterGroup filterGroup
    )
    {
        return isMatch(Objects.requireNonNull(fieldMap), criteria(Objects.requireNonNull(filterGroup)));
    }

    public boolean isMatch(
        final Row row,
        final SearchCriteria searchCriteria
    )
    {
        return searchCriteriaCondition.isEntityMatch(
            Objects.requireNonNull(row),
            Objects.requireNonNull(searchCriteria)
        );
    }

    public boolean isMatch(
        final Map<String, String> fieldMap,
        final SearchCriteria searchCriteria
    )
    {
        return searchCriteriaCondition.isEntityMatch(
            Objects.requireNonNull(fieldMap),
            Objects.requireNonNull(searchCriteria)
        );
    }

    public boolean isMatchAll(
        final Row row,
        final Filter... filters
    )
    {
        return isMatch(Objects.requireNonNull(row), allOf(filters));
    }

    public boolean isMatchAll(
        final Map<String, String> fieldMap,
        final Filter... filters
    )
    {
        return isMatch(Objects.requireNonNull(fieldMap), allOf(filters));
    }

    public static SearchCriteria allOf(final Filter... filters)
    {
        final Filter[] safeFilters = Objects.requireNonNull(filters);
        final FilterGroup[] groups = new FilterGroup[safeFilters.length];
        for (int index = 0; index < safeFilters.length; index++)
        {
            groups[index] = new FilterGroup(new Filter[] {Objects.requireNonNull(safeFilters[index])});
        }
        return criteria(groups);
    }

    public static SearchCriteria criteria(final FilterGroup... filterGroups)
    {
        return new SearchCriteria(
            Objects.requireNonNull(filterGroups),
            1,
            1,
            SORT_ORDERS
        );
    }
}
