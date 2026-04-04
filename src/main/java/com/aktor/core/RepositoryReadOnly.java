package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;

import java.util.Objects;

public class RepositoryReadOnly<Item extends Data<Key>, Key>
extends RepositoryWrapper<Item, Key>
{
    private final RepositoryReadOnlyMode mode;

    public RepositoryReadOnly(
        final Repository<Item, Key> repository,
        final RepositoryReadOnlyMode mode
    )
    {
        super(repository);
        this.mode = Objects.requireNonNull(mode);
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        if (mode == RepositoryReadOnlyMode.THROW)
        {
            throw new SaveException("Repository is read-only.");
        }
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        if (mode == RepositoryReadOnlyMode.THROW)
        {
            throw new DeleteException("Repository is read-only.");
        }
    }
}
