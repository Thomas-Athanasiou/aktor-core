package com.aktor.core.model;

import java.util.Objects;

public final class RouterDefaultFactory
implements RouterFactory
{
    private final EndpointProvider endpointProvider;

    public RouterDefaultFactory(final EndpointProvider endpointProvider)
    {
        this.endpointProvider = Objects.requireNonNull(endpointProvider);
    }

    @Override
    public Router create(final FactoryContext context, final String name)
    {
        return new RouterDefault(endpointProvider);
    }
}
