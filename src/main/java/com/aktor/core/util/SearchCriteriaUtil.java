package com.aktor.core.util;

import com.aktor.core.FilterGroup;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SortOrder;

import java.util.Arrays;

public final class SearchCriteriaUtil
{
    private static final FilterGroup[] FILTER_GROUPS = new FilterGroup[0];
    private static final SortOrder[] SORT_ORDERS = new SortOrder[0];

    private SearchCriteriaUtil()
    {
    }

    public static SearchCriteria setPageSize(final SearchCriteria searchCriteria, final int pageSize)
    {
        return new SearchCriteria(
            searchCriteria.filterGroups(),
            pageSize,
            searchCriteria.currentPage(),
            searchCriteria.sortOrders()
        );
    }

    public static SearchCriteria setCurrentPage(final SearchCriteria searchCriteria, final int currentPage)
    {
        return new SearchCriteria(
            searchCriteria.filterGroups(),
            searchCriteria.pageSize(),
            currentPage,
            searchCriteria.sortOrders()
        );
    }

    public static SearchCriteria addFilterGroup(final SearchCriteria searchCriteria, final FilterGroup filterGroup)
    {
        final FilterGroup[] currentFilterGroups = searchCriteria.filterGroups();
        final int originalLength = currentFilterGroups.length;
        final FilterGroup[] filterGroups = Arrays.copyOf(currentFilterGroups, originalLength + 1);
        filterGroups[originalLength] = filterGroup;
        return new SearchCriteria(
            filterGroups,
            searchCriteria.pageSize(),
            searchCriteria.currentPage(),
            searchCriteria.sortOrders()
        );
    }

    public static SearchCriteria clearFilterGroups(final SearchCriteria searchCriteria)
    {
        return new SearchCriteria(
            FILTER_GROUPS,
            searchCriteria.pageSize(),
            searchCriteria.currentPage(),
            searchCriteria.sortOrders()
        );
    }

    public static SearchCriteria addSortOrder(final SearchCriteria searchCriteria, final SortOrder sortOrder)
    {
        final SortOrder[] currentSortOrders = searchCriteria.sortOrders();
        final int originalLength = currentSortOrders.length;
        final SortOrder[] sortOrders = Arrays.copyOf(currentSortOrders, originalLength + 1);
        sortOrders[originalLength] = sortOrder;
        return new SearchCriteria(
            searchCriteria.filterGroups(),
            searchCriteria.pageSize(),
            searchCriteria.currentPage(),
            sortOrders
        );
    }

    public static SearchCriteria clearSortOrders(final SearchCriteria searchCriteria)
    {
        return new SearchCriteria(
            searchCriteria.filterGroups(),
            searchCriteria.pageSize(),
            searchCriteria.currentPage(),
            SORT_ORDERS
        );
    }
}

