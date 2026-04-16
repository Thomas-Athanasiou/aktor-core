package com.aktor.core;

import com.aktor.core.data.Scope;
import com.aktor.core.data.ScopeItem;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.service.Management;
import com.aktor.core.value.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class RepositoryScope<Item extends Data<Key>, Key>
extends RepositoryFrame<Item, Key>
{
    private static final SortOrder[] NO_SORT_ORDERS = new SortOrder[0];

    public RepositoryScope(final Management<ScopeItem<Key, Item>, Key> management, final Scope scope)
    {
        this(management, () -> scope);
    }

    public RepositoryScope(
        final Management<ScopeItem<Key, Item>, Key> management,
        final Supplier<? extends Scope> scopeSupplier
    )
    {
        super(
            getter(management, requireScopeSupplier(scopeSupplier)),
            saver(management, requireScopeSupplier(scopeSupplier)),
            deleter(management, requireScopeSupplier(scopeSupplier)),
            searcher(management, requireScopeSupplier(scopeSupplier))
        );
    }

    private static <Item extends Data<Key>, Key> Get<Item, Key> getter(
        final Management<ScopeItem<Key, Item>, Key> management,
        final Supplier<? extends Scope> scopeSupplier
    )
    {
        return key -> {
            final Scope scope = currentScope(scopeSupplier);
            final SearchResult<ScopeItem<Key, Item>> result;
            try
            {
                result = management.search(scopedKeyCriteria(scope, key));
            }
            catch (final SearchException exception)
            {
                throw new GetException(exception);
            }
            final List<ScopeItem<Key, Item>> items = result.items();
            if (items.isEmpty())
            {
                throw new GetException(
                    "The item with key identity of '" + key + "' in scope '" + scope.key() + "' doesn't exist, verify the item and try again"
                );
            }
            return items.get(0).payload();
        };
    }

    private static <Item extends Data<Key>, Key> Save<Item, Key> saver(
        final Management<ScopeItem<Key, Item>, Key> management,
        final Supplier<? extends Scope> scopeSupplier
    )
    {
        return item -> management.save(new ScopeItem<>(item.key(), currentScope(scopeSupplier), item));
    }

    private static <Item extends Data<Key>, Key> Delete<Item, Key> deleter(
        final Management<ScopeItem<Key, Item>, Key> management,
        final Supplier<? extends Scope> scopeSupplier
    )
    {
        return item -> management.delete(new ScopeItem<>(item.key(), currentScope(scopeSupplier), item));
    }

    private static <Item extends Data<Key>, Key> Search<Item, Key> searcher(
        final Management<ScopeItem<Key, Item>, Key> management,
        final Supplier<? extends Scope> scopeSupplier
    )
    {
        return searchCriteria -> {
            final Scope scope = currentScope(scopeSupplier);
            final SearchCriteria safeSearchCriteria = Objects.requireNonNull(searchCriteria, "searchCriteria");
            final SearchResult<ScopeItem<Key, Item>> result = management.search(scopedSearchCriteria(scope, safeSearchCriteria));
            final List<ScopeItem<Key, Item>> scopeItems = result.items();
            final List<Item> items = new ArrayList<>(scopeItems.size());
            for (final ScopeItem<Key, Item> scopeItem : scopeItems)
            {
                items.add(scopeItem.payload());
            }
            return new SearchResult<>(items, safeSearchCriteria, result.totalCount());
        };
    }

    private static <Key> SearchCriteria scopedKeyCriteria(final Scope scope, final Key key)
    {
        return new SearchCriteria(
            new FilterGroup[] {
                new FilterGroup(new Filter[] {
                    scopeFilter(scope),
                    new Filter("key", String.valueOf(key), ConditionType.EQUALS)
                })
            },
            1,
            1,
            NO_SORT_ORDERS
        );
    }

    private static SearchCriteria scopedSearchCriteria(
        final Scope scope,
        final SearchCriteria searchCriteria
    )
    {
        return addScopeFilter(transformSearchCriteria(searchCriteria), scope);
    }

    private static SearchCriteria transformSearchCriteria(final SearchCriteria searchCriteria)
    {
        return new SearchCriteria(
            transformFilterGroups(searchCriteria.filterGroups()),
            searchCriteria.pageSize(),
            searchCriteria.currentPage(),
            transformSortOrders(searchCriteria.sortOrders())
        );
    }

    private static SearchCriteria addScopeFilter(final SearchCriteria searchCriteria, final Scope scope)
    {
        final FilterGroup[] groups = searchCriteria.filterGroups();
        final FilterGroup[] next = new FilterGroup[groups.length + 1];
        next[0] = new FilterGroup(new Filter[] { scopeFilter(scope) });
        System.arraycopy(groups, 0, next, 1, groups.length);
        return new SearchCriteria(next, searchCriteria.pageSize(), searchCriteria.currentPage(), searchCriteria.sortOrders());
    }

    private static FilterGroup[] transformFilterGroups(final FilterGroup[] groups)
    {
        final FilterGroup[] next = new FilterGroup[groups.length];
        for (int index = 0; index < groups.length; index++)
        {
            next[index] = transformFilterGroup(groups[index]);
        }
        return next;
    }

    private static FilterGroup transformFilterGroup(final FilterGroup filterGroup)
    {
        final Filter[] filters = filterGroup.filters();
        final Filter[] next = new Filter[filters.length];
        for (int index = 0; index < filters.length; index++)
        {
            final Filter filter = filters[index];
            next[index] = new Filter(prefixPayloadField(filter.field()), filter.value(), filter.conditionType());
        }
        return new FilterGroup(next);
    }

    private static SortOrder[] transformSortOrders(final SortOrder[] sortOrders)
    {
        final SortOrder[] next = new SortOrder[sortOrders.length];
        for (int index = 0; index < sortOrders.length; index++)
        {
            final SortOrder sortOrder = sortOrders[index];
            next[index] = new SortOrder(prefixPayloadField(sortOrder.field()), sortOrder.direction());
        }
        return next;
    }

    private static String prefixPayloadField(final String field)
    {
        final String safeField = Objects.requireNonNull(field);
        if ("key".equals(safeField))
        {
            return safeField;
        }
        return safeField.startsWith("payload.") ? safeField : "payload." + safeField;
    }

    private static Filter scopeFilter(final Scope scope)
    {
        return new Filter("scope.key", scope.key(), ConditionType.EQUALS);
    }

    private static Scope currentScope(final Supplier<? extends Scope> scopeSupplier)
    {
        return Objects.requireNonNull(scopeSupplier.get(), "scope");
    }

    private static Supplier<? extends Scope> requireScopeSupplier(final Supplier<? extends Scope> scopeSupplier)
    {
        return Objects.requireNonNull(scopeSupplier, "scopeSupplier");
    }
}
