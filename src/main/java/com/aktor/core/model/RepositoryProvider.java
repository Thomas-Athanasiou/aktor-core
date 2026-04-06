package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;

import java.util.Objects;

public final class RepositoryProvider
{
    private static final Object[] OBJECTS = new Object[0];

    private final Configuration configuration;

    private final Object[] dependencies;

    public RepositoryProvider(final Configuration configuration, final Object... dependencies)
    {
        this.configuration = Objects.requireNonNull(configuration);
        this.dependencies = dependencies == null ? OBJECTS : dependencies.clone();
    }

    public static RepositoryProvider of(final Configuration configuration, final Object... dependencies)
    {
        return new RepositoryProvider(configuration, dependencies);
    }

    public Configuration configuration()
    {
        return configuration;
    }

    public <Dependency> Dependency require(final Class<Dependency> type)
    {
        final Class<Dependency> safeType = Objects.requireNonNull(type);
        for (final Object dependency : dependencies)
        {
            if (safeType.isInstance(dependency))
            {
                return safeType.cast(dependency);
            }
        }
        throw new IllegalArgumentException("No dependency available for type: " + safeType.getName());
    }

    public String requireConfiguration(final String path, final String label)
    {
        final String value = configuration.getString(Objects.requireNonNull(path));
        if (value == null)
        {
            throw new IllegalArgumentException(label + " is required at configuration path: " + path);
        }
        if (value.isBlank())
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
