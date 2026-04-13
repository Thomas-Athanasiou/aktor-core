package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.service.Management;

import java.util.Objects;

@FunctionalInterface
public interface ManagementFactory
extends Factory<ManagementRequest<?, ?>, Management<?, ?>>
{
    String CONFIG_SECTION = "entity";
    String CONFIG_MANAGEMENT = "management";
    String CONFIG_KIND = "kind";

    <Item extends Data<Key>, Key> Management<Item, Key> create(
        FactoryContext context,
        ManagementRequest<Item, Key> request
    );

    static ManagementFactory of(final ManagementProvider provider, final String name)
    {
        final ManagementProvider safeProvider = Objects.requireNonNull(provider);
        final String safeName = Objects.requireNonNull(name);

        final Configuration entity = Resolver.config(safeProvider.configuration(), CONFIG_SECTION, safeName);
        final Configuration management = entity.getConfiguration(CONFIG_MANAGEMENT);
        final String kind = management.getString(CONFIG_KIND);
        if (kind != null && !kind.isBlank())
        {
            return safeProvider.environment().load(ManagementFactory.class, kind.trim());
        }
        final String legacyKind = entity.getString(CONFIG_KIND);
        if (legacyKind != null && !legacyKind.isBlank())
        {
            return safeProvider.environment().load(ManagementFactory.class, legacyKind.trim());
        }
        throw new IllegalArgumentException(
            CONFIG_MANAGEMENT + "." + CONFIG_KIND + " is required for entity: " + safeName
        );
    }

    static ManagementProvider requireProvider(final FactoryContext context)
    {
        if (context instanceof final ManagementProvider provider)
        {
            return provider;
        }
        throw new IllegalArgumentException("ManagementFactory requires ManagementProvider context.");
    }

    @SuppressWarnings("unchecked")
    static <Item extends Data<Key>, Key> ManagementRequest<Item, Key> request(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        return new ManagementRequest<>(
            Objects.requireNonNull(name),
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(keyType)
        );
    }
}
