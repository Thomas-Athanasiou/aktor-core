package com.aktor.core;

import com.aktor.core.data.Event;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.FactoryContext;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryFactoryLoader;
import com.aktor.core.model.RepositoryProvider;
import com.aktor.core.model.RepositoryRequest;
import com.aktor.core.service.Broadcaster;

import java.util.Objects;

public class RepositoryEvent<Item extends Data<Key>, Key, EventItem extends Event<EventKey, Item>, EventKey>
extends RepositoryFrame<Item, Key>
{
    private static final String OPERATION_SAVE = "save";
    private static final String OPERATION_DELETE = "delete";

    private final String subject;
    private final Broadcaster<?, EventKey, Item, ?> broadcaster;
    private final Converter<Context<Item, Key>, EventItem> eventConverter;

    public RepositoryEvent(
        final Repository<Item, Key> repository,
        final String subject,
        final Broadcaster<?, EventKey, Item, ?> broadcaster,
        final Converter<Context<Item, Key>, EventItem> eventConverter
    )
    {
        super(repository::get, repository::save, repository::delete, repository::search);
        this.subject = Objects.requireNonNull(subject, "subject");
        this.broadcaster = Objects.requireNonNull(broadcaster);
        this.eventConverter = Objects.requireNonNull(eventConverter);
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        super.save(item);
        broadcast(toEvent(new Context<>(item, this.subject, OPERATION_SAVE)));
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        super.delete(item);
        try
        {
            broadcast(toEvent(new Context<>(item, this.subject, OPERATION_DELETE)));
        }
        catch (final SaveException exception)
        {
            throw new DeleteException("Failed to broadcast delete event", exception);
        }
    }

    private void broadcast(final EventItem event)
    {
        this.broadcaster.broadcast(event);
    }

    private EventItem toEvent(final Context<Item, Key> context) throws SaveException
    {
        try
        {
            return this.eventConverter.convert(context);
        }
        catch (final ConversionException exception)
        {
            throw new SaveException("Failed to convert repository event context", exception);
        }
    }

    public record Context<Item extends Data<Key>, Key>(Item item, String subject, String operation)
    {
        public Context
        {
            item = Objects.requireNonNull(item);
            subject = Objects.requireNonNull(subject);
            operation = Objects.requireNonNull(operation);
        }
    }

    public static final class Loader
    implements RepositoryFactoryLoader
    {
        @Override
        public String kind()
        {
            return "event";
        }

        @Override
        public RepositoryFactory load(final Environment environment)
        {
            return new Factory();
        }
    }

    public static final class Factory
    implements RepositoryFactory
    {
        @Override
        @SuppressWarnings("unchecked")
        public <Item extends Data<Key>, Key> Repository<Item, Key> createTyped(
            final FactoryContext context,
            final RepositoryRequest<Item, Key> request
        )
        {
            final RepositoryProvider provider = RepositoryFactory.requireProvider(context);
            final Configuration wrapper = provider.configuration().entity(request.name()).getConfiguration("wrapper");
            final String source = firstNonBlank(
                wrapper.optString("source", null),
                wrapper.optString("repository", null),
                wrapper.optString("target", null)
            );
            if (source == null || source.isBlank())
            {
                throw new IllegalArgumentException("Event wrapper source is required for repository: " + request.name());
            }
            final Broadcaster<?, Key, Item, ?> broadcaster = (Broadcaster<?, Key, Item, ?>) provider.require(Broadcaster.class);
            final Repository<Item, Key> repository = provider.instance(
                source.trim(),
                request.itemType(),
                request.keyType(),
                request.relationProviderResolver()
            );
            return new RepositoryEvent<>(
                repository,
                firstNonBlank(wrapper.optString("subject", null), request.name()),
                broadcaster,
                new PersistenceEventConverter<>()
            );
        }

        private static String firstNonBlank(final String first, final String second, final String third)
        {
            if (first != null && !first.isBlank())
            {
                return first.trim();
            }
            if (second != null && !second.isBlank())
            {
                return second.trim();
            }
            if (third != null && !third.isBlank())
            {
                return third.trim();
            }
            return null;
        }

        private static String firstNonBlank(final String first, final String fallback)
        {
            if (first != null && !first.isBlank())
            {
                return first.trim();
            }
            return fallback == null ? null : fallback.trim();
        }
    }
}
