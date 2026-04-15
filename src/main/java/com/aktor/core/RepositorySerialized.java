package com.aktor.core;

import com.aktor.core.exception.*;
import com.aktor.core.model.CollectionProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class RepositorySerialized<Item extends Data<Key>, Key>
extends SearchExecutorRelational<Item, Key>
implements Repository<Item, Key>
{
    private static final int BATCH_SIZE = 1024;
    private final int batchSize;

    protected abstract String getByPath(final String path) throws GetException;

    protected abstract List<String> getBatch(final int from, final int to) throws SearchException;

    protected abstract void assignToPath(final String path, final String data) throws SaveException;

    protected abstract void removeFromPath(final String path) throws DeleteException;

    private final Converter<Item, String> serializer;

    final Converter<String, Item> deserializer;

    protected RepositorySerialized(
        final Converter<Item, String> serializer,
        final Converter<String, Item> deserializer,
        final CollectionProcessor<Item, Key> processor,
        final int batchSize
    )
    {
        super(processor);
        this.serializer = Objects.requireNonNull(serializer);
        this.deserializer = Objects.requireNonNull(deserializer);
        this.batchSize = batchSize;
    }

    protected RepositorySerialized(
        final Converter<Item, String> serializer,
        final Converter<String, Item> deserializer,
        final CollectionProcessor<Item, Key> processor
    )
    {
        this(serializer, deserializer, processor, BATCH_SIZE);
    }

    @Override
    public final Item get(final Key key) throws GetException
    {
        final String data = getByPath(getDataKey(key));
        if (data == null || data.isBlank())
        {
            throw new GetException(
                "The item with key " + key + " that was requested doesn't exist, verify the item and try again"
            );
        }

        final Item item;
        try
        {
            item = deserializer.convert(data);
        }
        catch (final ConversionException exception)
        {
            throw new GetException(exception);
        }

        return item;
    }


    @Override
    public final void save(final Item item) throws SaveException
    {
        try
        {
            assignToPath(getDataKey(item.key()), serializer.convert(item));
        }
        catch (final ConversionException exception)
        {
            throw new SaveException(exception);
        }
    }

    @Override
    public final void delete(final Item item) throws DeleteException
    {
        removeFromPath(getDataKey(item.key()));
    }

    protected List<String> getAllData() throws SearchException
    {
        final List<String> result = new ArrayList<>();
        int from = 0;
        List<String> batch;
        do
        {
            batch = getBatch(from, from + batchSize);
            result.addAll(batch);
            from += batchSize;
        }
        while (!batch.isEmpty());
        return result;
    }

    private Iterable<Item> loadItems() throws SearchException
    {
        try
        {
            return iterateAllItems(getAllData());
        }
        catch (final DeserializationRuntimeException exception)
        {
            throw new SearchException(exception.getCause());
        }
    }

    @Override
    protected SearchSource<Item, Key> searchSource(final SearchCriteria searchCriteria) throws SearchException
    {
        return this::loadItems;
    }

    @Override
    protected SearchResult<Item> searchNative(final SearchCriteria searchCriteria) throws SearchException
    {
        return search(searchCriteria, searchSource(searchCriteria));
    }

    protected final List<String> snapshotData(
        final Iterable<String> keys,
        final int from,
        final int to,
        final Function<String, String> valueLookup
    )
    {
        final List<String> orderedKeys = sortKeys(keys);
        final int size = orderedKeys.size();
        final int safeFrom = Math.max(0, Math.min(from, size));
        final int safeTo = Math.max(safeFrom, Math.min(to, size));
        final List<String> result = new ArrayList<>(safeTo - safeFrom);
        for (int index = safeFrom; index < safeTo; index++)
        {
            result.add(valueLookup.apply(orderedKeys.get(index)));
        }
        return result;
    }

    protected final List<String> snapshotData(
        final Iterable<String> keys,
        final Function<String, String> valueLookup
    )
    {
        final List<String> orderedKeys = sortKeys(keys);
        final List<String> result = new ArrayList<>(orderedKeys.size());
        for (final String key : orderedKeys)
        {
            result.add(valueLookup.apply(key));
        }
        return result;
    }

    private static List<String> sortKeys(final Iterable<String> keys)
    {
        final List<String> orderedKeys = new ArrayList<>();
        for (final String key : keys)
        {
            orderedKeys.add(key);
        }
        orderedKeys.sort(String::compareTo);
        return orderedKeys;
    }

    private Iterable<Item> iterateAllItems(final Iterable<String> data)
    {
        return () -> new Iterator<>()
        {
            private final Iterator<String> iterator = data.iterator();

            @Override
            public boolean hasNext()
            {
                return iterator.hasNext();
            }

            @Override
            public Item next()
            {
                final String next = iterator.next();
                try
                {
                    return deserializer.convert(next);
                }
                catch (final ConversionException exception)
                {
                    throw new DeserializationRuntimeException(exception);
                }
            }
        };
    }

    private String getDataKey(final Key key)
    {
        return String.valueOf(key);
    }

    private static final class DeserializationRuntimeException
    extends RuntimeException
    {
        private DeserializationRuntimeException(final ConversionException cause)
        {
            super(cause);
        }

        @Override
        public synchronized ConversionException getCause()
        {
            return (ConversionException) super.getCause();
        }
    }
}
