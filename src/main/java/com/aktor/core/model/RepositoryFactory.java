package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.model.RelationProviderResolver;

import java.util.Objects;

@FunctionalInterface
public interface RepositoryFactory
{
    <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        RepositoryProvider provider,
        String name,
        Class<Item> itemType,
        Class<Key> keyType
    );

    default <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final RepositoryProvider provider,
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        return repository(provider, name, itemType, keyType);
    }

    static RepositoryFactory of(final RepositoryProvider provider, final String name)
    {
        final RepositoryProvider safeProvider = Objects.requireNonNull(provider);
        final String safeName = Objects.requireNonNull(name);

        final Configuration entities = safeProvider.configuration().getConfiguration("entity");
        final Configuration entity = entities.has(safeName) ? entities.getConfiguration(safeName) : safeProvider.configuration().getConfiguration(safeName);
        if (entity.has("aggregate"))
        {
            final Configuration aggregate = entity.getConfiguration("aggregate");
            final String kind = firstNonBlank(aggregate.getString("kind"), "aggregate");
            return safeProvider.environment().load(RepositoryFactory.class, kind);
        }
        if (entity.has("wrapper"))
        {
            final Configuration wrapper = entity.getConfiguration("wrapper");
            final String kind = firstNonBlank(wrapper.getString("kind"), "readonly");
            return safeProvider.environment().load(RepositoryFactory.class, kind);
        }
        final Configuration storage = entity.getConfiguration("storage");
        final String kind = firstNonBlank(storage.getString("kind"), entity.getString("kind"));
        final String safeKind = kind == null || kind.isBlank() ? "sqlite" : kind.trim();

        return safeProvider.environment().load(RepositoryFactory.class, safeKind);
    }

    private static String firstNonBlank(final String first, final String second)
    {
        if (first != null && !first.isBlank())
        {
            return first.trim();
        }
        if (second != null && !second.isBlank())
        {
            return second.trim();
        }
        return null;
    }
}
