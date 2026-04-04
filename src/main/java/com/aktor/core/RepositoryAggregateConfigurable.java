package com.aktor.core;

import com.aktor.core.model.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RepositoryAggregateConfigurable<Item extends Data<Key>, Key>
extends RepositoryAggregate<Item, Key>
{
    private final ConfiguredRepositoryListResolver<Item, Key> repositoryResolver;

    public RepositoryAggregateConfigurable(
        final Map<String, Repository<Item, Key>> repositoryMap,
        final Configuration configuration
    )
    {
        super(new java.util.ArrayList<>(repositoryMap.values()));
        this.repositoryResolver = new ConfiguredRepositoryListResolver<>(
            Objects.requireNonNull(repositoryMap),
            Objects.requireNonNull(configuration)
        );
    }

    @Override
    protected final List<Repository<Item, Key>> getRepositories()
    {
        return repositoryResolver.getRepositories();
    }
}
