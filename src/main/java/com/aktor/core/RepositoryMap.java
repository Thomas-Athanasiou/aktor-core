package com.aktor.core;

import com.aktor.core.model.CollectionProcessor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RepositoryMap<Item extends Data<Key>, Key>
extends RepositorySerialized<Item, Key>
{
    protected final Map<String, String> map;

    public RepositoryMap(
        final Converter<Item, String> serializer,
        final Converter<String, Item> deserializer,
        final CollectionProcessor<Item, Key> processor,
        final Map<String, String> map
    )
    {
        super(serializer, deserializer, processor);
        this.map = Objects.requireNonNull(map);
    }

    @Override
    protected String getByPath(final String path)
    {
        return map.get(path);
    }

    @Override
    protected List<String> getBatch(final int from, final int to)
    {
        return snapshotData(map.keySet(), from, to, map::get);
    }

    @Override
    protected List<String> getAllData()
    {
        return snapshotData(map.keySet(), map::get);
    }

    @Override
    protected void assignToPath(final String path, final String data)
    {
        map.put(path, data);
    }

    @Override
    protected void removeFromPath(final String path)
    {
        map.remove(path);
    }
}
