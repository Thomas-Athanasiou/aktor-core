package com.aktor.core.model;

import com.aktor.core.Data;

import java.util.Objects;

public record RepositoryRequest<Item extends Data<Key>, Key>(
    String name,
    Class<Item> itemType,
    Class<Key> keyType,
    RelationProviderResolver<Key> relationProviderResolver
)
{
    public RepositoryRequest
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(itemType);
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(relationProviderResolver);
    }
}
