package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.FilterGroup;
import com.aktor.core.Repository;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.SortOrder;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.CollectionProcessor;
import com.aktor.core.model.RelationProcessor;
import com.aktor.core.value.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ManagementRelational<Item extends Data<Key>, Key>
implements Management<Item, Key>
{
    private static final FilterGroup[] FILTER_GROUPS = new FilterGroup[0];
    private static final SortOrder[] SORT_ORDERS = new SortOrder[0];
    private static final Filter[] FILTERS = new Filter[0];
    private static final SearchCriteria SEARCH_CRITERIA = new SearchCriteria(FILTER_GROUPS, Integer.MAX_VALUE, 1, SORT_ORDERS);

    protected final Repository<Item, Key> repository;
    protected final RelationProcessor<Key> relationProcessor;
    protected final CollectionProcessor<Item, Key> processor;

    protected ManagementRelational(
        final Repository<Item, Key> repository,
        final RelationProcessor<Key> relationProcessor,
        final CollectionProcessor<Item, Key> processor
    )
    {
        this.repository = Objects.requireNonNull(repository);
        this.relationProcessor = Objects.requireNonNull(relationProcessor);
        this.processor = Objects.requireNonNull(processor);
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        return repository.get(key);
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        final SearchCriteria safeSearchCriteria = searchCriteria == null ? SEARCH_CRITERIA : searchCriteria;
        if (!hasRelationFilters(safeSearchCriteria))
        {
            return searchNative(safeSearchCriteria);
        }

        final SearchCriteria candidateCriteria = withoutPagination(withoutRelationFilters(safeSearchCriteria));
        final SearchResult<Item> candidates = searchNative(candidateCriteria);
        return processor.process(candidates.items(), safeSearchCriteria);
    }

    @Override
    public final boolean exists(final Key key)
    {
        try
        {
            return Objects.equals(get(key).key(), key);
        }
        catch (final GetException exception)
        {
            return false;
        }
    }

    protected abstract SearchResult<Item> searchNative(final SearchCriteria searchCriteria) throws SearchException;

    private boolean hasRelationFilters(final SearchCriteria searchCriteria)
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

    private SearchCriteria withoutRelationFilters(final SearchCriteria searchCriteria)
    {
        if (!hasRelationFilters(searchCriteria))
        {
            return searchCriteria;
        }

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

    private SearchCriteria withoutPagination(final SearchCriteria searchCriteria)
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

    private boolean isRelationField(final CharSequence field)
    {
        return field != null && field.toString().contains(".");
    }
}
