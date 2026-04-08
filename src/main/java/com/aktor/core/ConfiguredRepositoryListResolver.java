package com.aktor.core;

import com.aktor.core.model.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ConfiguredRepositoryListResolver<Item extends Data<Key>, Key>
{
    private final Map<String, Repository<Item, Key>> map;
    private final Configuration configuration;
    private volatile List<Repository<Item, Key>> cachedRepositories = null;

    ConfiguredRepositoryListResolver(
        final Map<String, Repository<Item, Key>> map,
        final Configuration configuration
    )
    {
        this.map = Objects.requireNonNull(map);
        this.configuration = Objects.requireNonNull(configuration);
    }

    List<Repository<Item, Key>> getRepositories()
    {
        List<Repository<Item, Key>> repositories = cachedRepositories;
        if (repositories == null)
        {
            repositories = resolveRepositories();
            cachedRepositories = repositories;
        }
        return repositories;
    }

    private List<Repository<Item, Key>> resolveRepositories()
    {
        final String[] methods = configuration.getConfiguration("storage").keys();
        final Collection<Repository<Item, Key>> repositories = new ArrayList<>(methods.length);
        for (final String method : methods)
        {
            final Repository<Item, Key> repository = map.get(method);
            if (repository != null)
            {
                repositories.add(repository);
            }
        }
        return List.copyOf(repositories);
    }
}
