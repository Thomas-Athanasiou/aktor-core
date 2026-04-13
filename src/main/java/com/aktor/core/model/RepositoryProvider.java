package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.RepositoryAggregate;
import com.aktor.core.RepositoryCache;
import com.aktor.core.RepositoryReadOnly;
import com.aktor.core.RepositoryRotating;

import java.util.Objects;
import java.util.ServiceLoader;

public final class RepositoryProvider
extends Provider<RepositoryFactory>
{
    public RepositoryProvider(final Configuration configuration, final Object... dependencies)
    {
        super(
            Objects.requireNonNull(configuration),
            RepositoryFactory.class,
            combineLoaders(
                ServiceLoader.load(RepositoryFactoryLoader.class),
                new RepositoryAggregate.Loader(),
                new RepositoryCache.Loader(),
                new RepositoryReadOnly.Loader(),
                new RepositoryRotating.Loader()
            ),
            dependencies
        );
    }

    public static RepositoryProvider of(final Configuration configuration, final Object... dependencies)
    {
        return new RepositoryProvider(configuration, dependencies);
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
            (safeName, context, safeRequest) -> RepositoryFactory.of(this, safeName).create(context, safeRequest)
        );
    }

}
