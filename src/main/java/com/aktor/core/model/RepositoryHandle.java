package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;

import java.util.Objects;

public class RepositoryHandle<Item extends Data<Key>, Key>
{
    private final RepositoryProvider<Item, Key> provider;
    private final Class<Item> itemType;
    private final Class<Key> keyType;
    private final RelationProviderResolver<Key> relationProviderResolver;

    protected RepositoryHandle(
        final Configuration configuration,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final RelationProviderResolver<Key> relationProviderResolver,
        final Object... dependencies
    )
    {
        this.provider = new RepositoryProvider<>(Objects.requireNonNull(configuration), dependencies);
        this.itemType = Objects.requireNonNull(itemType);
        this.keyType = Objects.requireNonNull(keyType);
        this.relationProviderResolver = Objects.requireNonNull(relationProviderResolver);
    }

    public static <Item extends Data<Key>, Key> RepositoryHandle<Item, Key> of(
        final Configuration configuration,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final Object... dependencies
    )
    {
        return new RepositoryHandle<>(
            configuration,
            itemType,
            keyType,
            new RelationProviderResolver<>(),
            dependencies
        );
    }

    public static <Item extends Data<Key>, Key> RepositoryHandle<Item, Key> of(
        final Configuration configuration,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final RelationProviderResolver<Key> relationProviderResolver,
        final Object... dependencies
    )
    {
        return new RepositoryHandle<>(
            configuration,
            itemType,
            keyType,
            relationProviderResolver,
            dependencies
        );
    }

    public Repository<Item, Key> repository(final String name)
    {
        return provider.instance(Objects.requireNonNull(name), itemType, keyType, relationProviderResolver);
    }

    public RepositoryProvider<Item, Key> provider()
    {
        return provider;
    }
}
