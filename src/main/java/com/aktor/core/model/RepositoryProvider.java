package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.RepositoryAggregate;
import com.aktor.core.RepositoryCache;
import com.aktor.core.RepositoryReadOnly;
import com.aktor.core.RepositoryRotating;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public final class RepositoryProvider
extends Provider<RepositoryFactory>
{
    public RepositoryProvider(final Configuration configuration, final Object... dependencies)
    {
        this(configuration, List.of(), dependencies);
    }

    public RepositoryProvider(
        final Configuration configuration,
        final Iterable<? extends Loader<? extends RepositoryFactory>> extraLoaders,
        final Object... dependencies
    )
    {
        super(
            Objects.requireNonNull(configuration),
            RepositoryFactory.class,
            loaders(extraLoaders),
            dependencies
        );
    }

    public static RepositoryProvider of(
        final Configuration configuration,
        final Object... dependencies
    )
    {
        return new RepositoryProvider(configuration, dependencies);
    }

    public static RepositoryProvider of(
        final Configuration configuration,
        final Iterable<? extends Loader<? extends RepositoryFactory>> extraLoaders,
        final Object... dependencies
    )
    {
        return new RepositoryProvider(configuration, extraLoaders, dependencies);
    }

    public <Dependency> Dependency require(final Class<Dependency> type)
    {
        return super.environment().require(Objects.requireNonNull(type));
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> instance(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        return instance(name, itemType, keyType, new RelationProviderResolver<>());
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> instance(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        final RepositoryRequest<Item, Key> request = RepositoryFactory.request(
            Objects.requireNonNull(name),
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(keyType),
            Objects.requireNonNull(relationProviderResolver)
        );
        return super.instance(
            request.name(),
            request,
            this::createRepository
        );
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> instance(final RepositoryRequest<Item, Key> request)
    {
        final RepositoryRequest<Item, Key> safeRequest = Objects.requireNonNull(request);
        return super.instance(
            safeRequest.name(),
            safeRequest,
            this::createRepository
        );
    }

    private <Item extends Data<Key>, Key> Repository<Item, Key> createRepository(
        final String name,
        final FactoryContext context,
        final RepositoryRequest<Item, Key> request
    )
    {
        return RepositoryFactory.of(this, name).createTyped(context, request);
    }

    private static Iterable<? extends Loader<? extends RepositoryFactory>> loaders(
        final Iterable<? extends Loader<? extends RepositoryFactory>> extraLoaders
    )
    {
        return combineLoaders(
            ServiceLoader.load(RepositoryFactoryLoader.class),
            combineLoaders(
                extraLoaders,
                List.of(
                    new RepositoryAggregate.Loader(),
                    new RepositoryCache.Loader(),
                    new com.aktor.core.RepositoryPrimaryFallback.Loader(),
                    new RepositoryReadOnly.Loader(),
                    new RepositoryRotating.Loader()
                )
            )
        );
    }
}
