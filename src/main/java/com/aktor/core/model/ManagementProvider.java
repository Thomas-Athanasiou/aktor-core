package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.service.ManagementFactoryDirect;
import com.aktor.core.service.ManagementFactoryRepository;
import com.aktor.core.service.Management;

import java.util.ServiceLoader;
import java.util.Objects;

public final class ManagementProvider
implements FactoryContext
{
    private final RepositoryProvider repositories;

    public ManagementProvider(final RepositoryProvider repositories)
    {
        this.repositories = Objects.requireNonNull(repositories);
        final Environment environment = repositories.environment();
        environment.registerLoader(ManagementFactory.class, new ManagementFactoryDirect.Loader());
        environment.registerLoader(ManagementFactory.class, new ManagementFactoryRepository.Loader());
        for (final ManagementFactoryLoader loader : ServiceLoader.load(ManagementFactoryLoader.class))
        {
            environment.registerLoader(ManagementFactory.class, loader);
        }
    }

    public static ManagementProvider of(final RepositoryProvider repositories)
    {
        return new ManagementProvider(repositories);
    }

    public RepositoryProvider repositories()
    {
        return repositories;
    }

    public Configuration configuration()
    {
        return repositories.configuration();
    }

    public Environment environment()
    {
        return repositories.environment();
    }

    public <Dependency> Dependency require(final Class<Dependency> type)
    {
        return repositories.require(type);
    }

    public String requireConfiguration(final String key, final String label)
    {
        return repositories.requireConfiguration(key, label);
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        return repositories.repository(name, itemType, keyType);
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final com.aktor.core.model.RelationProviderResolver<Key> relationProviderResolver
    )
    {
        return repositories.repository(name, itemType, keyType, relationProviderResolver);
    }

    public <Item extends Data<Key>, Key> Management<Item, Key> management(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        final ManagementRequest<Item, Key> request = ManagementFactory.request(
            Objects.requireNonNull(name),
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(keyType)
        );
        return ManagementFactory.of(this, request.name()).create(this, request);
    }
}
