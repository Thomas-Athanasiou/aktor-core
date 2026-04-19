package com.aktor.core.model;

import java.util.Objects;

@FunctionalInterface
public interface EndpointFactory
extends Factory<String, Endpoint>
{
    String CONFIG_SECTION = "endpoint";

    static EndpointFactory of(final EndpointProvider provider, final String name)
    {
        final EndpointProvider safeProvider = Objects.requireNonNull(provider);
        final String safeName = Objects.requireNonNull(name);

        final String safeKind = Resolver.kind(
            safeProvider.configuration(),
            CONFIG_SECTION,
            safeName,
            CONFIG_SECTION
        );

        return safeProvider.environment().load(EndpointFactory.class, safeKind);
    }
}
