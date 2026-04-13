package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.RepositoryAggregate;
import com.aktor.core.RepositoryCache;
import com.aktor.core.RepositoryReadOnly;
import com.aktor.core.RepositoryRotating;
import com.aktor.core.model.RelationProviderResolver;

import java.sql.CallableStatement;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public final class RepositoryProvider
extends Provider<RepositoryFactory, RepositoryFactory>
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

    public String requireConfiguration(final String key, final String label)
    {
        final String value = configuration().getString(Objects.requireNonNull(key));
        if (value == null)
        {
            throw new IllegalArgumentException(label + " is required at configuration key: " + key);
        }
        else if (value.isBlank())
        {
            throw new IllegalArgumentException(label + " cannot be blank at configuration key: " + key);
        }
        return value;
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        return repository(name, itemType, keyType, new RelationProviderResolver<>());
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
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
            safeName -> RepositoryFactory.of(this, safeName),
            (factory, safeRequest) -> factory.create(this, safeRequest)
        );
    }

}
