package com.aktor.core.model;

import com.aktor.core.Data;

import java.util.Objects;

public record ManagementRequest<Item extends Data<Key>, Key>(
    String name,
    Class<Item> itemType,
    Class<Key> keyType
)
{
    public ManagementRequest
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(itemType);
        Objects.requireNonNull(keyType);
    }
}
