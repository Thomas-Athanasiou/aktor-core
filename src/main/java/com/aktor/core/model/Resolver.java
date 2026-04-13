package com.aktor.core.model;

import java.util.Objects;

public final class Resolver
{
    private Resolver()
    {
    }

    public static Configuration config(final Configuration configuration, final String section, final String name)
    {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(name);

        final Configuration parent = configuration.getConfiguration(Objects.requireNonNull(section));
        return parent.has(name) ? parent.getConfiguration(name) : configuration.getConfiguration(name);
    }

    public static String kind(
        final Configuration configuration,
        final String section,
        final String name,
        final String defaultKind
    )
    {
        final Configuration config = config(configuration, section, name);
        final String kind = firstNonBlank(config.getString("kind"), config.getString("type"));
        return kind == null || kind.isBlank() ? defaultKind : kind.trim();
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
