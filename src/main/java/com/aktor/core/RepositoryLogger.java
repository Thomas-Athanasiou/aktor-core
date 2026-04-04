package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.model.Logger;

import java.util.Objects;

public class RepositoryLogger<Item extends Data<Key>, Key>
extends RepositoryWrapper<Item, Key>
{
    private final Logger logger;

    public RepositoryLogger(
        final Repository<Item, Key> repository,
        final Logger logger
    )
    {
        super(repository);
        this.logger = Objects.requireNonNull(logger);
    }

    @Override
    public final Item get(final Key key) throws GetException
    {
        final Item item = super.get(key);
        logger.debug("Getting item of " + item.getClass().getSimpleName() + " by key " + key);
        return item;
    }

    @Override
    public final void save(final Item item) throws SaveException
    {
        final String simpleName = item.getClass().getSimpleName();
        try
        {
            super.save(item);
            logger.debug("Saved item of " + item.getClass().getSimpleName());
        }
        catch (final SaveException saveException)
        {
            logger.error("Error saving item of " + simpleName + ", Message: " + saveException.getMessage());
            throw saveException;
        }
    }

    @Override
    public final void delete(final Item item) throws DeleteException
    {
        final String simpleName = item.getClass().getSimpleName();
        try
        {
            super.delete(item);
            logger.debug("Deleted item of " + simpleName);
        }
        catch (final DeleteException deleteException)
        {
            logger.error("Error deleting item of " + simpleName + ", Message: " + deleteException.getMessage());
            throw deleteException;
        }
    }
}
