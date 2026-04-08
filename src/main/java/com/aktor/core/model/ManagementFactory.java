package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.service.Management;

import java.util.Objects;

@FunctionalInterface
public interface ManagementFactory
{
    <Item extends Data<Key>, Key> Management<Item, Key> management(
        ManagementProvider provider,
        String name,
        Class<Item> itemType,
        Class<Key> keyType
    );

    static ManagementFactory of(final ManagementProvider provider, final String name)
    {
        final ManagementProvider safeProvider = Objects.requireNonNull(provider);
        final String safeName = Objects.requireNonNull(name);

        final Configuration entities = safeProvider.configuration().getConfiguration("entity");
        final Configuration entity = entities.has(safeName) ? entities.getConfiguration(safeName) : safeProvider.configuration().getConfiguration(safeName);
        final Configuration management = entity.getConfiguration("management");
        final String kind = firstNonBlank(management.getString("kind"), entity.getString("kind"));
        final String safeKind = kind == null || kind.isBlank() ? "repository" : kind.trim();

        return safeProvider.environment().load(ManagementFactory.class, safeKind);
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
