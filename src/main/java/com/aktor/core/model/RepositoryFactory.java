package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;

@FunctionalInterface
public interface RepositoryFactory
{
    <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        RepositoryProvider provider,
        String name,
        Class<Item> itemType,
        Class<Key> keyType
    );

    static RepositoryFactory of(final RepositoryProvider provider, final String name)
    {
        final RepositoryProvider safeProvider = java.util.Objects.requireNonNull(provider);
        final String safeName = java.util.Objects.requireNonNull(name);

        // TODO THIS CONFIG PART IS NASTY
        final String storage = safeProvider.requireConfiguration(safeName + ".storage", "storage");
        final String kind = safeProvider.requireConfiguration("storage." + storage + ".kind", "storage kind");

        return safeProvider.environment().load(RepositoryFactory.class, kind);
    }
}
