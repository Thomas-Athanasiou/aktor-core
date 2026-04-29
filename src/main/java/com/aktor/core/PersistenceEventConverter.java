package com.aktor.core;

import com.aktor.core.data.PersistenceEvent;
import com.aktor.core.exception.ConversionException;

import java.util.Objects;

public final class PersistenceEventConverter<Item extends Data<Key>, Key>
implements Converter<RepositoryEvent.Context<Item, Key>, PersistenceEvent<Key, Item>>
{
    @Override
    public PersistenceEvent<Key, Item> convert(final RepositoryEvent.Context<Item, Key> context)
    throws ConversionException
    {
        final RepositoryEvent.Context<Item, Key> safeContext = Objects.requireNonNull(context, "context");
        final Item item = safeContext.item();
        return new PersistenceEvent<>(
            item.key(),
            safeContext.subject(),
            safeContext.operation(),
            item,
            System.currentTimeMillis()
        );
    }
}
