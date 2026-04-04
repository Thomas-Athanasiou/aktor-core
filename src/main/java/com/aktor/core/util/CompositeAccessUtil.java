package com.aktor.core.util;

import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.exception.SaveException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompositeAccessUtil
{
    private CompositeAccessUtil()
    {
        super();
    }

    public static <Source, Item, Key> Item getAggregate(
        final Iterable<Source> sources,
        final Key key,
        final Getter<Source, Key, Item> getter
    ) throws GetException
    {
        Item item = null;
        GetException cause = null;
        for (final Source source : sources)
        {
            try
            {
                item = getter.get(source, key);
                break;
            }
            catch (final GetException getException)
            {
                cause = getException;
            }
        }

        if (item == null)
        {
            throw new GetException(cause);
        }

        return item;
    }

    public static <Source, Item, Key> Item getCache(
        final Iterable<Source> sources,
        final Key key,
        final int cacheWriteSourceCount,
        final Getter<Source, Key, Item> getter,
        final Saver<Source, Item> saver
    ) throws GetException
    {
        Item item = null;
        GetException cause = null;
        final List<Source> cacheSources = cacheWriteSourceCount > 0
            ? new ArrayList<>(Math.min(cacheWriteSourceCount, 4))
            : Collections.emptyList();
        int sourceIndex = 0;
        for (final Source source : sources)
        {
            try
            {
                item = getter.get(source, key);
                saveInSources(item, cacheSources, saver);
                break;
            }
            catch (final GetException getException)
            {
                if (sourceIndex < cacheWriteSourceCount)
                {
                    cacheSources.add(source);
                }
                cause = getException;
            }
            catch (final SaveException exception)
            {
                throw new GetException(exception);
            }
            sourceIndex++;
        }

        if (item == null)
        {
            throw new GetException(cause);
        }

        return item;
    }

    public static com.aktor.core.SearchCriteria requireSearchCriteria(final com.aktor.core.SearchCriteria searchCriteria)
    throws SearchException
    {
        if (searchCriteria == null)
        {
            throw new SearchException("searchCriteria cannot be null");
        }
        return searchCriteria;
    }

    private static <Source, Item> void saveInSources(
        final Item item,
        final Iterable<Source> sources,
        final Saver<Source, Item> saver
    ) throws SaveException
    {
        for (final Source source : sources)
        {
            saver.save(source, item);
        }
    }

    @FunctionalInterface
    public interface Getter<Source, Key, Item>
    {
        Item get(Source source, Key key) throws GetException;
    }

    @FunctionalInterface
    public interface Saver<Source, Item>
    {
        void save(Source source, Item item) throws SaveException;
    }
}
