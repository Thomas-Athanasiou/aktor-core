package com.aktor.core;

import com.aktor.core.exception.SearchException;
import com.aktor.core.model.CollectionProcessor;
import com.aktor.core.value.Filter;

import java.util.ArrayList;
import java.util.List;

public abstract class SearchExecutorRelational<Item extends Data<Key>, Key>
extends SearchExecutor<Item, Key>
{
    private static final FilterGroup[] FILTER_GROUPS = new FilterGroup[0];
    private static final SortOrder[] SORT_ORDERS = new SortOrder[0];
    private static final Filter[] FILTERS = new Filter[0];
    private static final SearchCriteria SEARCH_CRITERIA = new SearchCriteria(FILTER_GROUPS, Integer.MAX_VALUE, 1, SORT_ORDERS);

    protected SearchExecutorRelational(CollectionProcessor<Item, Key> processor)
    {
        super(processor);
    }

    @Override
    public final SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        final SearchCriteria safeSearchCriteria = searchCriteria == null ? SEARCH_CRITERIA : searchCriteria;
        if (!hasRelationFilters(safeSearchCriteria))
        {
            return searchNative(safeSearchCriteria);
        }

        final SearchCriteria candidateCriteria = withoutPagination(withoutRelationFilters(safeSearchCriteria));
        return search(onlyRelationFilters(safeSearchCriteria), searchSource(candidateCriteria));
    }

    @Override
    protected final SearchSource<Item, Key> source()
    {
        throw new UnsupportedOperationException("Relational search requires search criteria.");
    }

    protected abstract SearchSource<Item, Key> searchSource(final SearchCriteria searchCriteria) throws SearchException;

    protected abstract SearchResult<Item> searchNative(final SearchCriteria searchCriteria) throws SearchException;

    protected final boolean hasRelationFilters(final SearchCriteria searchCriteria)
    {
        if (searchCriteria != null)
        {
            for (final FilterGroup group : searchCriteria.filterGroups())
            {
                for (final Filter filter : group.filters())
                {
                    if (isRelationField(filter.field()))
                    {
                        return true;
                    }
                }
            }
            for (final SortOrder sortOrder : searchCriteria.sortOrders())
            {
                if (isRelationField(sortOrder.field()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    protected final SearchCriteria withoutRelationFilters(final SearchCriteria searchCriteria)
    {
        if (hasRelationFilters(searchCriteria))
        {
            final List<FilterGroup> groups = new ArrayList<>();
            for (final FilterGroup group : searchCriteria.filterGroups())
            {
                final List<Filter> filters = new ArrayList<>();
                for (final Filter filter : group.filters())
                {
                    if (!isRelationField(filter.field()))
                    {
                        filters.add(filter);
                    }
                }
                if (!filters.isEmpty())
                {
                    groups.add(new FilterGroup(filters.toArray(FILTERS)));
                }
            }

            final List<SortOrder> sortOrders = new ArrayList<>();
            for (final SortOrder sortOrder : searchCriteria.sortOrders())
            {
                if (!isRelationField(sortOrder.field()))
                {
                    sortOrders.add(sortOrder);
                }
            }

            return new SearchCriteria(
                groups.toArray(FILTER_GROUPS),
                searchCriteria.pageSize(),
                searchCriteria.currentPage(),
                sortOrders.toArray(SORT_ORDERS)
            );
        }
        return searchCriteria;
    }

    protected final SearchCriteria onlyRelationFilters(final SearchCriteria searchCriteria)
    {
        if (hasRelationFilters(searchCriteria))
        {
            final List<FilterGroup> groups = new ArrayList<>();
            for (final FilterGroup group : searchCriteria.filterGroups())
            {
                final List<Filter> filters = new ArrayList<>();
                for (final Filter filter : group.filters())
                {
                    if (isRelationField(filter.field()))
                    {
                        filters.add(filter);
                    }
                }
                if (!filters.isEmpty())
                {
                    groups.add(new FilterGroup(filters.toArray(FILTERS)));
                }
            }

            final List<SortOrder> sortOrders = new ArrayList<>();
            for (final SortOrder sortOrder : searchCriteria.sortOrders())
            {
                if (isRelationField(sortOrder.field()))
                {
                    sortOrders.add(sortOrder);
                }
            }

            return new SearchCriteria(
                groups.toArray(FILTER_GROUPS),
                searchCriteria.pageSize(),
                searchCriteria.currentPage(),
                sortOrders.toArray(SORT_ORDERS)
            );
        }
        return new SearchCriteria(FILTER_GROUPS, Integer.MAX_VALUE, 1, SORT_ORDERS);
    }

    protected final SearchCriteria withoutPagination(final SearchCriteria searchCriteria)
    {
        if (searchCriteria == null)
        {
            return null;
        }
        return new SearchCriteria(
            searchCriteria.filterGroups(),
            Integer.MAX_VALUE,
            1,
            searchCriteria.sortOrders()
        );
    }

    protected final boolean isRelationField(final CharSequence field)
    {
        return field != null && field.toString().contains(".");
    }
}
