package com.aktor.core;

import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.FactoryContext;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryFactoryLoader;
import com.aktor.core.model.RepositoryProvider;
import com.aktor.core.model.RepositoryRequest;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

import java.util.Objects;
import java.util.function.IntSupplier;

public final class RepositoryRotating<Item extends Data<Key>, Key>
extends RepositoryFrame<Item, Key>
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
        super(repository::get, repository::save, repository::delete, repository::search);
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

    public static final class Factory
    implements RepositoryFactory
    {
        @Override
        public <Item extends Data<Key>, Key> Repository<Item, Key> createTyped(
            final FactoryContext context,
            final RepositoryRequest<Item, Key> request
        )
        {
            final RepositoryProvider provider = RepositoryFactory.requireProvider(context);
            final Configuration wrapper = wrapper(provider.configuration(), request.name());
            final String source = firstNonBlank(
                singleSource(wrapper.getConfiguration("sources")),
                wrapper.getString("source"),
                wrapper.getString("repository")
            );
            if (source == null || source.isBlank())
            {
                throw new IllegalArgumentException("Wrapper source is required for repository: " + request.name());
            }
            return new RepositoryRotating<>(
                provider.instance(
                    source,
                    request.itemType(),
                    request.keyType(),
                    request.relationProviderResolver()
                ),
                () -> Integer.parseInt(Objects.requireNonNull(firstNonBlank(wrapper.getString("maxItems"), "0"))),
                firstNonBlank(wrapper.getString("rotationField"), "key"),
                Boolean.parseBoolean(firstNonBlank(wrapper.getString("rotationDirection"), "true"))
            );
        }

        private static Configuration wrapper(final Configuration configuration, final String name)
        {
            final Configuration entities = configuration.getConfiguration("entity");
            if (entities.has(name))
            {
                return entities.getConfiguration(name).getConfiguration("wrapper");
            }
            return configuration.getConfiguration(name).getConfiguration("wrapper");
        }

        private static String singleSource(final Configuration sources)
        {
            final String[] keys = sources.keys();
            return keys.length == 0 ? null : keys[0];
        }

        private static String firstNonBlank(final String ...arguments)
        {
            for (String argument : arguments)
            {
                if (argument != null && !argument.isBlank())
                {
                    return argument.trim();
                }
            }
            return null;
        }
    }

    public static final class Loader
    implements RepositoryFactoryLoader
    {
        @Override
        public String kind()
        {
            return "rotating";
        }

        @Override
        public RepositoryFactory load(final Environment environment)
        {
            return new Factory();
        }
    }
}
