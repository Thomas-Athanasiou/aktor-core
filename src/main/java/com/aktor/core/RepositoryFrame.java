package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

import java.util.Objects;

public class RepositoryFrame<Item extends Data<Key>, Key>
implements Repository<Item, Key>
{
    private final Get<Item, Key> getter;

    private final Save<Item, Key> saver;

    private final Delete<Item, Key> deleter;

    private final Search<Item, Key> searcher;

    public RepositoryFrame(
        final Get<Item, Key> getter,
        final Save<Item, Key> saver,
        final Delete<Item, Key> deleter,
        final Search<Item, Key> searcher
    )
    {
        this.getter = Objects.requireNonNull(getter);
        this.saver = Objects.requireNonNull(saver);
        this.deleter = Objects.requireNonNull(deleter);
        this.searcher = Objects.requireNonNull(searcher);
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        return getter.get(key);
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return searcher.search(searchCriteria);
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        saver.save(item);
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        deleter.delete(item);
    }
}
