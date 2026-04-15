package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.service.ManagementFactoryDirect;
import com.aktor.core.service.ManagementFactoryRepository;
import com.aktor.core.service.Management;

import java.util.List;
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
                List.of(
                    new ManagementFactoryDirect.Loader(),
                    new ManagementFactoryRepository.Loader()
                )
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

    public RepositoryProvider repositories()
    {
        return repositories;
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
            (safeName, context, safeRequest) -> ManagementFactory.of(this, safeName).createTyped(context, safeRequest)
        );
    }
}
