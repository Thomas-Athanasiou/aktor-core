package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

import java.util.Objects;

public final class ManagementDelegate<Item extends Data<Key>, Key>
implements Management<Item, Key>
{
    private Management<Item, Key> delegate = null;

    public void setDelegate(final Management<Item, Key> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    private Management<Item, Key> requireDelegate()
    {
        final Management<Item, Key> currentDelegate = delegate;
        if (currentDelegate == null)
        {
            throw new IllegalStateException("Management delegate is not ready.");
        }
        return currentDelegate;
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        return requireDelegate().get(key);
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return requireDelegate().search(searchCriteria);
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        requireDelegate().delete(item);
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        requireDelegate().save(item);
    }

    @Override
    public boolean exists(final Key key)
    {
        return requireDelegate().exists(key);
    }
}
