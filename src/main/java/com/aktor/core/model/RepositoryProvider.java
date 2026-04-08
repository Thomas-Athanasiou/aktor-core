package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.RepositoryAggregate;
import com.aktor.core.RepositoryCache;
import com.aktor.core.RepositoryReadOnly;
import com.aktor.core.RepositoryRotating;
import com.aktor.core.model.RelationProviderResolver;

import java.util.ServiceLoader;

import java.util.Objects;

public final class RepositoryProvider
{
    private final Configuration configuration;
    private final Environment environment;

    public RepositoryProvider(final Configuration configuration, final Object... dependencies)
    {
        this.configuration = Objects.requireNonNull(configuration);
        this.environment = new EnvironmentDefault();
        if (dependencies != null)
        {
            for (final Object dependency : dependencies)
            {
                environment.put(dependency);
            }
        }
        environment.registerLoader(RepositoryFactory.class, new RepositoryAggregate.Loader());
        environment.registerLoader(RepositoryFactory.class, new RepositoryCache.Loader());
        environment.registerLoader(RepositoryFactory.class, new RepositoryReadOnly.Loader());
        environment.registerLoader(RepositoryFactory.class, new RepositoryRotating.Loader());
        for (final RepositoryFactoryLoader loader : ServiceLoader.load(RepositoryFactoryLoader.class))
        {
            environment.registerLoader(RepositoryFactory.class, loader);
        }
    }

    public static RepositoryProvider of(final Configuration configuration, final Object... dependencies)
    {
        return new RepositoryProvider(configuration, dependencies);
    }

    public Configuration configuration()
    {
        return configuration;
    }

    public Environment environment()
    {
        return environment;
    }

    public <Dependency> Dependency require(final Class<Dependency> type)
    {
        return environment.require(Objects.requireNonNull(type));
    }

    public String requireConfiguration(final String key, final String label)
    {
        final String value = configuration.getString(Objects.requireNonNull(key));
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
        return RepositoryFactory.of(this, Objects.requireNonNull(name)).repository(
            this,
            name,
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(keyType),
            Objects.requireNonNull(relationProviderResolver)
        );
    }
}
