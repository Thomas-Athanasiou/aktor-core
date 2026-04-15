package com.aktor.core;

import com.aktor.core.data.Event;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.SaveException;

import java.util.Objects;

public class RepositoryEvent<Item extends Data<Key>, Key, EventItem extends Event<EventKey, EventTarget>, EventKey, EventTarget>
extends RepositoryFrame<Item, Key>
{
    private final Repository<EventItem, EventKey> eventRepository;
    private final Converter<Item, EventItem> eventConverter;

    public RepositoryEvent(
        final Repository<Item, Key> repository,
        final Repository<EventItem, EventKey> eventRepository,
        final Converter<Item, EventItem> eventConverter
    )
    {
        super(repository::get, repository::save, repository::delete, repository::search);
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.eventConverter = Objects.requireNonNull(eventConverter);
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        super.save(item);
        final EventItem event = toEvent(item);
        this.eventRepository.save(event);
    }

    private EventItem toEvent(final Item item) throws SaveException
    {
        try
        {
            return this.eventConverter.convert(item);
        }
        catch (final ConversionException exception)
        {
            throw new SaveException("Failed to convert item to event", exception);
        }
    }
}
