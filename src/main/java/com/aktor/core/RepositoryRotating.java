package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

public class RepositoryRotating<Item extends Data<Key>, Key>
extends RepositoryWrapper<Item, Key>
{
    private static final FilterGroup[] NO_FILTERS = new FilterGroup[0];

    private final IntSupplier maxItemsSupplier;

    private final String rotationField;

    private final boolean rotationDirection;

    public RepositoryRotating(
        final Repository<Item, Key> repository,
        final IntSupplier maxItemsSupplier,
        final String rotationField,
        final boolean rotationDirection
    )
    {
        super(repository);
        this.maxItemsSupplier = Objects.requireNonNull(maxItemsSupplier);
        this.rotationField = Objects.requireNonNull(rotationField);
        this.rotationDirection = rotationDirection;
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
                final int totalCount = super.search(
                    new SearchCriteria(NO_FILTERS, 1, 1, new SortOrder[]{new SortOrder(this.rotationField, !this.rotationDirection)})
                ).totalCount();

                final int overflow = totalCount - maxItems;
                if (overflow >= 1)
                {
                    final List<Item> oldest = super.search(
                        new SearchCriteria(
                            NO_FILTERS,
                            overflow,
                            1,
                            new SortOrder[]{new SortOrder(this.rotationField, this.rotationDirection)}
                        )
                    ).items();

                    for (final Item oldestItem : oldest)
                    {
                        super.delete(oldestItem);
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
