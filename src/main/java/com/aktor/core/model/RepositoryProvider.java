package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;

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

    public String requireConfiguration(final String path, final String label)
    {
        final String value = configuration.getString(Objects.requireNonNull(path));
        if (value == null)
        {
            throw new IllegalArgumentException(label + " is required at configuration path: " + path);
        }
        else if (value.isBlank())
        {
            throw new IllegalArgumentException(label + " cannot be blank at configuration path: " + path);
        }
        return value;
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        return RepositoryFactory.of(this, Objects.requireNonNull(name)).repository(
            this,
            name,
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(keyType)
        );
    }
}
