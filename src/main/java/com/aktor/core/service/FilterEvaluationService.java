package com.aktor.core.service;

import com.aktor.core.DataRow;
import com.aktor.core.FilterGroup;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SortOrder;
import com.aktor.core.model.SearchCriteriaCondition;
import com.aktor.core.value.Filter;

import java.util.Map;
import java.util.Objects;

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

    public boolean isMatch(
        final DataRow dataRow,
        final Filter filter
    )
    {
        return isMatch(Objects.requireNonNull(dataRow), allOf(Objects.requireNonNull(filter)));
    }

    public boolean isMatch(
        final Map<String, String> fieldMap,
        final Filter filter
    )
    {
        return isMatch(Objects.requireNonNull(fieldMap), allOf(Objects.requireNonNull(filter)));
    }

    public boolean isMatch(
        final DataRow dataRow,
        final FilterGroup filterGroup
    )
    {
        return isMatch(Objects.requireNonNull(dataRow), criteria(Objects.requireNonNull(filterGroup)));
    }

    public boolean isMatch(
        final Map<String, String> fieldMap,
        final FilterGroup filterGroup
    )
    {
        return isMatch(Objects.requireNonNull(fieldMap), criteria(Objects.requireNonNull(filterGroup)));
    }

    public boolean isMatch(
        final DataRow dataRow,
        final SearchCriteria searchCriteria
    )
    {
        return searchCriteriaCondition.isEntityMatch(
            Objects.requireNonNull(dataRow),
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
        final DataRow dataRow,
        final Filter... filters
    )
    {
        return isMatch(Objects.requireNonNull(dataRow), allOf(filters));
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
