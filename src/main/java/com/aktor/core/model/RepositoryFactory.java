package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;

import java.util.Objects;

@FunctionalInterface
public interface RepositoryFactory
extends Factory<RepositoryRequest<?, ?>, Repository<?, ?>>
{
    String CONFIG_SECTION = "entity";
    String CONFIG_STORAGE = "storage";
    String CONFIG_WRAPPER = "wrapper";
    String CONFIG_AGGREGATE = "aggregate";
    String CONFIG_KIND = "kind";

    <Item extends Data<Key>, Key> Repository<Item, Key> create(
        FactoryContext context,
        RepositoryRequest<Item, Key> request
    );

    static RepositoryFactory of(final RepositoryProvider provider, final String name)
    {
        final RepositoryProvider safeProvider = Objects.requireNonNull(provider);
        final String safeName = Objects.requireNonNull(name);

        final Configuration entity = Resolver.config(safeProvider.configuration(), CONFIG_SECTION, safeName);
        if (entity.has(CONFIG_AGGREGATE))
        {
            final Configuration aggregate = entity.getConfiguration(CONFIG_AGGREGATE);
            final String kind = requireKind(aggregate, CONFIG_AGGREGATE + "." + CONFIG_KIND);
            return safeProvider.environment().load(RepositoryFactory.class, kind);
        }
        if (entity.has(CONFIG_WRAPPER))
        {
            final Configuration wrapper = entity.getConfiguration(CONFIG_WRAPPER);
            final String kind = requireKind(wrapper, CONFIG_WRAPPER + "." + CONFIG_KIND);
            return safeProvider.environment().load(RepositoryFactory.class, kind);
        }
        final Configuration storage = entity.getConfiguration(CONFIG_STORAGE);
        final String kind = storage.getString(CONFIG_KIND);
        if (kind != null && !kind.isBlank())
        {
            return safeProvider.environment().load(RepositoryFactory.class, kind.trim());
        }
        final String legacyKind = entity.getString(CONFIG_KIND);
        if (legacyKind != null && !legacyKind.isBlank())
        {
            return safeProvider.environment().load(RepositoryFactory.class, legacyKind.trim());
        }
        throw new IllegalArgumentException(
            CONFIG_STORAGE + "." + CONFIG_KIND + " is required for repository entity: " + safeName
        );
    }

    static RepositoryProvider requireProvider(final FactoryContext context)
    {
        if (context instanceof final RepositoryProvider provider)
        {
            return provider;
        }
        throw new IllegalArgumentException("RepositoryFactory requires RepositoryProvider context.");
    }

    @SuppressWarnings("unchecked")
    static <Item extends Data<Key>, Key> RepositoryRequest<Item, Key> request(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        return new RepositoryRequest<>(
            Objects.requireNonNull(name),
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(keyType),
            Objects.requireNonNull(relationProviderResolver)
        );
    }

    private static String requireKind(final Configuration configuration, final String keyLabel)
    {
        final String value = configuration.getString(CONFIG_KIND);
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException("Missing required " + keyLabel + " configuration.");
        }
        return value.trim();
    }
}
