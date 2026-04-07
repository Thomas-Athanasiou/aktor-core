package com.aktor.core.util;

import com.aktor.core.Data;
import com.aktor.core.CacheWritePolicy;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.SaveProvider;
import com.aktor.core.model.SearchProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CompositeSearchUtil
{
    public enum TotalCountMode
    {
        DISJOINT_SUM,
        DEDUP_EXACT
    }

    private CompositeSearchUtil()
    {
        super();
    }

    public static <Source, Item extends Data<Key>, Key> SearchResult<Item> searchAggregate(
        final Iterable<Source> sourceList,
        final SearchCriteria searchCriteria,
        final SearchProvider<Source, Item> searchProvider
    ) throws SearchException
    {
        return searchAggregate(sourceList, searchCriteria, searchProvider, TotalCountMode.DEDUP_EXACT);
    }

    public static <Source, Item extends Data<Key>, Key> SearchResult<Item> searchAggregate(
        final Iterable<Source> sourceList,
        final SearchCriteria searchCriteria,
        final SearchProvider<Source, Item> searchProvider,
        final TotalCountMode totalCountMode
    ) throws SearchException
    {
        return search(sourceList, searchCriteria, searchProvider, null, 0, totalCountMode);
    }

    public static <Source, Item extends Data<Key>, Key> SearchResult<Item> searchCache(
        final Iterable<Source> sourceList,
        final SearchCriteria searchCriteria,
        final SearchProvider<Source, Item> searchProvider,
        final SaveProvider<Source, Item> saveProvider
    ) throws SearchException
    {
        return searchCache(
            sourceList,
            searchCriteria,
            searchProvider,
            saveProvider,
            Integer.MAX_VALUE,
            TotalCountMode.DEDUP_EXACT
        );
    }

    public static <Source, Item extends Data<Key>, Key> SearchResult<Item> searchCache(
        final Iterable<Source> sourceList,
        final SearchCriteria searchCriteria,
        final SearchProvider<Source, Item> searchProvider,
        final SaveProvider<Source, Item> saveProvider,
        final int cacheWriteSourceCount
    ) throws SearchException
    {
        return searchCache(
            sourceList,
            searchCriteria,
            searchProvider,
            saveProvider,
            cacheWriteSourceCount,
            TotalCountMode.DEDUP_EXACT
        );
    }

    public static <Source, Item extends Data<Key>, Key> SearchResult<Item> searchCache(
        final Iterable<Source> sourceList,
        final SearchCriteria searchCriteria,
        final SearchProvider<Source, Item> searchProvider,
        final SaveProvider<Source, Item> saveProvider,
        final CacheWritePolicy cacheWritePolicy
    ) throws SearchException
    {
        return searchCache(
            sourceList,
            searchCriteria,
            searchProvider,
            saveProvider,
            Objects.requireNonNull(cacheWritePolicy).writeSourceCount(),
            TotalCountMode.DEDUP_EXACT
        );
    }

    public static <Source, Item extends Data<Key>, Key> SearchResult<Item> searchCache(
        final Iterable<Source> sourceList,
        final SearchCriteria searchCriteria,
        final SearchProvider<Source, Item> searchProvider,
        final SaveProvider<Source, Item> saveProvider,
        final int cacheWriteSourceCount,
        final TotalCountMode totalCountMode
    ) throws SearchException
    {
        return search(
            sourceList,
            searchCriteria,
            searchProvider,
            Objects.requireNonNull(saveProvider),
            cacheWriteSourceCount,
            totalCountMode
        );
    }

    private static <Source, Item extends Data<Key>, Key> SearchResult<Item> search(
        final Iterable<Source> sourceList,
        final SearchCriteria searchCriteria,
        final SearchProvider<Source, Item> searchProvider,
        final SaveProvider<Source, Item> saveProvider,
        final int cacheWriteSourceCount,
        final TotalCountMode totalCountMode
    ) throws SearchException
    {
        Objects.requireNonNull(sourceList);
        Objects.requireNonNull(searchCriteria);
        Objects.requireNonNull(searchProvider);
        Objects.requireNonNull(totalCountMode);

        final Map<Key, Item> itemMap = new LinkedHashMap<>();
        final List<Source> cacheList = saveProvider == null ? null : new ArrayList<>();
        final int pageSize = Math.max(1, searchCriteria.pageSize());
        final int currentPage = Math.max(1, searchCriteria.currentPage());
        final int offset = safeOffset(pageSize, currentPage);
        final int needed = safeAdd(offset, pageSize);
        final int safeCacheWriteSourceCount = Math.max(0, cacheWriteSourceCount);
        final Set<Key> uniqueKeys = totalCountMode == TotalCountMode.DEDUP_EXACT ? new HashSet<>() : null;

        int totalCountDisjoint = 0;
        int sourceIndex = 0;

        for (final Source source : sourceList)
        {
            final SearchResult<Item> firstPageResult = searchProvider.search(
                source,
                withPage(searchCriteria, pageSize, 1)
            );
            totalCountDisjoint = safeAdd(totalCountDisjoint, Math.max(0, firstPageResult.totalCount()));

            try
            {
                collectItems(firstPageResult.items(), itemMap, cacheList, saveProvider, needed, uniqueKeys);
                final int maxPages = maxPages(firstPageResult.totalCount(), pageSize);
                final boolean shouldReadAllPages = totalCountMode == TotalCountMode.DEDUP_EXACT;
                for (int page = 2; page <= maxPages && (shouldReadAllPages || itemMap.size() < needed); page++)
                {
                    final SearchResult<Item> pageResult = searchProvider.search(
                        source,
                        withPage(searchCriteria, pageSize, page)
                    );
                    collectItems(pageResult.items(), itemMap, cacheList, saveProvider, needed, uniqueKeys);
                }
            }
            catch (final SaveException exception)
            {
                throw new SearchException(exception);
            }
            appendCacheSource(cacheList, source, sourceIndex, safeCacheWriteSourceCount);
            sourceIndex++;
        }

        final int totalCount = totalCountMode == TotalCountMode.DISJOINT_SUM
            ? totalCountDisjoint
            : uniqueKeys.size();

        final List<Item> mergedItems = new ArrayList<>(itemMap.values());
        final int fromIndex = Math.min(offset, mergedItems.size());
        final int toIndex = Math.min(safeAdd(fromIndex, pageSize), mergedItems.size());
        final List<Item> pageItems = mergedItems.subList(fromIndex, toIndex);

        return new SearchResult<>(
            pageItems,
            searchCriteria,
            totalCount
        );
    }

    private static <Source, Item extends Data<Key>, Key> void collectItems(
        final Iterable<Item> items,
        final Map<Key, Item> itemMap,
        final Iterable<Source> cacheList,
        final SaveProvider<Source, Item> saveProvider,
        final int needed,
        final Collection<Key> uniqueKeys
    ) throws SaveException
    {
        for (final Item item : items)
        {
            final Key key = item.key();
            if (uniqueKeys != null)
            {
                uniqueKeys.add(key);
            }
            if (itemMap.size() < needed && itemMap.putIfAbsent(key, item) == null && cacheList != null)
            {
                saveInSources(item, cacheList, saveProvider);
            }
        }
    }

    private static <Source> void appendCacheSource(
        final Collection<Source> cacheList,
        final Source source,
        final int sourceIndex,
        final int cacheWriteSourceCount
    )
    {
        if (cacheList != null)
        {
            if (sourceIndex < cacheWriteSourceCount)
            {
                cacheList.add(source);
            }
        }
    }

    private static int safeOffset(final int pageSize, final int currentPage)
    {
        final long currentPageIndex = Math.max(0L, (long) currentPage - 1L);
        final long offsetLong = currentPageIndex * (long) pageSize;
        return offsetLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offsetLong;
    }

    private static int safeAdd(final int left, final int right)
    {
        final long sum = (long) left + (long) right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    private static int maxPages(final int totalCount, final int pageSize)
    {
        if (totalCount <= 0)
        {
            return 0;
        }
        return (int) ((totalCount + (long) pageSize - 1L) / (long) pageSize);
    }

    private static SearchCriteria withPage(final SearchCriteria searchCriteria, final int pageSize, final int currentPage)
    {
        return new SearchCriteria(
            searchCriteria.filterGroups(),
            pageSize,
            currentPage,
            searchCriteria.sortOrders()
        );
    }

    private static <Source, Item> void saveInSources(
        final Item item,
        final Iterable<Source> sourceList,
        final SaveProvider<Source, Item> saveProvider
    ) throws SaveException
    {
        for (final Source source : sourceList)
        {
            saveProvider.save(source, item);
        }
    }
}
