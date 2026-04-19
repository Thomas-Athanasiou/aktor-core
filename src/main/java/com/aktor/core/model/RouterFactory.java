package com.aktor.core.model;

import java.util.Objects;

@FunctionalInterface
public interface RouterFactory
extends Factory<String, Router>
{
    String CONFIG_SECTION = "router";

    static RouterFactory of(final RouterProvider provider, final String name)
    {
        return Objects.requireNonNull(provider).environment().load(
            RouterFactory.class,
            Resolver.kind(provider.configuration(), CONFIG_SECTION, Objects.requireNonNull(name), "default")
        );
    }

    static Configuration config(final RouterProvider provider, final String name)
    {
        return Resolver.config(Objects.requireNonNull(provider).configuration(), CONFIG_SECTION, Objects.requireNonNull(name));
    }
}
