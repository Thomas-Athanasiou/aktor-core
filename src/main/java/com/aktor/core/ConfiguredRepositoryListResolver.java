package com.aktor.core;

import com.aktor.core.model.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ConfiguredRepositoryListResolver<Item extends Data<Key>, Key>
{
    private final Map<String, Repository<Item, Key>> repositoryMap;
    private final Configuration configuration;
    private volatile List<Repository<Item, Key>> cachedRepositories;

    ConfiguredRepositoryListResolver(
        final Map<String, Repository<Item, Key>> repositoryMap,
        final Configuration configuration
    )
    {
        this.repositoryMap = Objects.requireNonNull(repositoryMap);
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
        final String[] methods = configuration.getStrings("storage/method");
        final List<Repository<Item, Key>> repositories = new ArrayList<>(methods.length);
        for (final String method : methods)
        {
            final Repository<Item, Key> repository = repositoryMap.get(method);
            if (repository != null)
            {
                repositories.add(repository);
            }
        }
        return List.copyOf(repositories);
    }
}
