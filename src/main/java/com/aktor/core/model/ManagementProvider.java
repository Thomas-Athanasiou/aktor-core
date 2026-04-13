package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.service.ManagementFactoryDirect;
import com.aktor.core.service.ManagementFactoryRepository;
import com.aktor.core.service.Management;

import java.util.ServiceLoader;
import java.util.Objects;

public final class ManagementProvider
extends Provider<ManagementFactory>
{
    private final RepositoryProvider repositories;

    public ManagementProvider(final RepositoryProvider repositories)
    {
        super(
            Objects.requireNonNull(repositories).configuration(),
            ManagementFactory.class,
            combineLoaders(
                ServiceLoader.load(ManagementFactoryLoader.class),
                new ManagementFactoryDirect.Loader(),
                new ManagementFactoryRepository.Loader()
            ),
            repositories
        );
        this.repositories = repositories;
    }

    public static ManagementProvider of(final RepositoryProvider repositories)
    {
        return new ManagementProvider(repositories);
    }

    public Configuration configuration()
    {
        return super.configuration();
    }

    public Environment environment()
    {
        return super.environment();
    }

    public <Dependency> Dependency require(final Class<Dependency> type)
    {
        return repositories.require(type);
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        return repositories.instance(name, itemType, keyType);
    }

    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final com.aktor.core.model.RelationProviderResolver<Key> relationProviderResolver
    )
    {
        return repositories.instance(name, itemType, keyType, relationProviderResolver);
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
        return super.instance(
            request.name(),
            request,
            (safeName, context, safeRequest) -> ManagementFactory.of(this, safeName).create(context, safeRequest)
        );
    }
}
