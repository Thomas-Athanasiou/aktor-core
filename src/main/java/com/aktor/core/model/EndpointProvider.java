package com.aktor.web.http;

import com.aktor.core.model.Configuration;
import com.aktor.core.model.Provider;

import java.util.Objects;
import java.util.ServiceLoader;

public final class EndpointProvider
extends Provider<EndpointFactory>
{
    public EndpointProvider(final Configuration configuration, final Object... dependencies)
    {
        super(
            Objects.requireNonNull(configuration),
            EndpointFactory.class,
            ServiceLoader.load(EndpointFactoryLoader.class),
            dependencies
        );
    }

    public static EndpointProvider of(final Configuration configuration, final Object... dependencies)
    {
        return new EndpointProvider(configuration, dependencies);
    }

    public Endpoint endpoint(final String name)
    {
        return super.instance(
            Objects.requireNonNull(name),
            (safeName, context, route) -> EndpointFactory.of(this, safeName).create(context, route)
        );
    }
}
