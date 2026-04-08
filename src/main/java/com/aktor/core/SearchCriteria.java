package com.aktor.core;

import java.util.Arrays;
import java.util.Objects;

public record SearchCriteria(
    FilterGroup[] filterGroups,
    int pageSize,
    int currentPage,
    SortOrder[] sortOrders
)
{
    private static final FilterGroup[] EMPTY_FILTER_GROUPS = new FilterGroup[0];
    private static final SortOrder[] EMPTY_SORT_ORDERS = new SortOrder[0];

    public SearchCriteria
    {
        filterGroups = Arrays.copyOf(Objects.requireNonNull(filterGroups), filterGroups.length);
        if (pageSize < 1)
        {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (currentPage < 1)
        {
            throw new IllegalArgumentException("currentPage must be greater than 0");
        }
        sortOrders = Arrays.copyOf(Objects.requireNonNull(sortOrders), sortOrders.length);
    }

    @Override
    public FilterGroup[] filterGroups()
    {
        return Arrays.copyOf(filterGroups, filterGroups.length);
    }

    @Override
    public SortOrder[] sortOrders()
    {
        return Arrays.copyOf(sortOrders, sortOrders.length);
    }

    public static SearchCriteria setPageSize(final SearchCriteria searchCriteria, final int pageSize)
    {
        final SearchCriteria safeSearchCriteria = Objects.requireNonNull(searchCriteria);
        return new SearchCriteria(safeSearchCriteria.filterGroups(), pageSize, safeSearchCriteria.currentPage(), safeSearchCriteria.sortOrders());
    }

    public static SearchCriteria setCurrentPage(final SearchCriteria searchCriteria, final int currentPage)
    {
        final SearchCriteria safeSearchCriteria = Objects.requireNonNull(searchCriteria);
        return new SearchCriteria(safeSearchCriteria.filterGroups(), safeSearchCriteria.pageSize(), currentPage, safeSearchCriteria.sortOrders());
    }

    public static SearchCriteria addFilterGroup(final SearchCriteria searchCriteria, final FilterGroup filterGroup)
    {
        final SearchCriteria safeSearchCriteria = Objects.requireNonNull(searchCriteria);
        final FilterGroup safeFilterGroup = Objects.requireNonNull(filterGroup);
        final FilterGroup[] currentFilterGroups = safeSearchCriteria.filterGroups();
        final FilterGroup[] next = Arrays.copyOf(currentFilterGroups, currentFilterGroups.length + 1);
        next[currentFilterGroups.length] = safeFilterGroup;
        return new SearchCriteria(next, safeSearchCriteria.pageSize(), safeSearchCriteria.currentPage(), safeSearchCriteria.sortOrders());
    }

    public static SearchCriteria clearFilterGroups(final SearchCriteria searchCriteria)
    {
        final SearchCriteria safeSearchCriteria = Objects.requireNonNull(searchCriteria);
        return new SearchCriteria(EMPTY_FILTER_GROUPS, safeSearchCriteria.pageSize(), safeSearchCriteria.currentPage(), safeSearchCriteria.sortOrders());
    }

    public static SearchCriteria addSortOrder(final SearchCriteria searchCriteria, final SortOrder sortOrder)
    {
        final SearchCriteria safeSearchCriteria = Objects.requireNonNull(searchCriteria);
        final SortOrder safeSortOrder = Objects.requireNonNull(sortOrder);
        final SortOrder[] currentSortOrders = safeSearchCriteria.sortOrders();
        final SortOrder[] next = Arrays.copyOf(currentSortOrders, currentSortOrders.length + 1);
        next[currentSortOrders.length] = safeSortOrder;
        return new SearchCriteria(safeSearchCriteria.filterGroups(), safeSearchCriteria.pageSize(), safeSearchCriteria.currentPage(), next);
    }

    public static SearchCriteria clearSortOrders(final SearchCriteria searchCriteria)
    {
        final SearchCriteria safeSearchCriteria = Objects.requireNonNull(searchCriteria);
        return new SearchCriteria(safeSearchCriteria.filterGroups(), safeSearchCriteria.pageSize(), safeSearchCriteria.currentPage(), EMPTY_SORT_ORDERS);
    }
}
