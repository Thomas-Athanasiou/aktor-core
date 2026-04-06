package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

public final class RepositoryRotating<Item extends Data<Key>, Key>
extends RepositoryWrapper<Item, Key>
{
    private static final FilterGroup[] NO_FILTERS = new FilterGroup[0];

    private final SortOrder[] sortOrders;

    private final IntSupplier maxItemsSupplier;

    public RepositoryRotating(
        final Repository<Item, Key> repository,
        final IntSupplier maxItemsSupplier,
        final String rotationField,
        final boolean rotationDirection
    )
    {
        super(repository);
        this.maxItemsSupplier = Objects.requireNonNull(maxItemsSupplier);
        this.sortOrders = new SortOrder[]
        {
            new SortOrder(Objects.requireNonNull(rotationField), !rotationDirection)
        };
    }

    @Override
    public synchronized void save(final Item item) throws SaveException
    {
        super.save(item);
        rotateNow();
    }

    public synchronized void rotateNow() throws SaveException
    {
        final int maxItems = this.maxItemsSupplier.getAsInt();
        if (maxItems >= 1)
        {
            try
            {
                final int totalCount = super.search(new SearchCriteria(NO_FILTERS, 1, 1, sortOrders)).totalCount();
                final int overflow = totalCount - maxItems;
                if (overflow >= 1)
                {
                    for (final Item item : super.search(new SearchCriteria(NO_FILTERS, overflow, 1, sortOrders)).items())
                    {
                        super.delete(item);
                    }
                }
            }
            catch (final SearchException | DeleteException exception)
            {
                throw new SaveException("Failed to rotate repository", exception);
            }
        }
    }
}
