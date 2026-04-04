package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

import java.util.Objects;

public class RepositoryWrapper<Item extends Data<Key>, Key>
implements Repository<Item, Key>
{
    private final Repository<Item, Key> repository;

    public RepositoryWrapper(final Repository<Item, Key> repository)
    {
        super();
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        return repository.get(key);
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return repository.search(searchCriteria);
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        repository.save(item);
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        repository.delete(item);
    }
}
