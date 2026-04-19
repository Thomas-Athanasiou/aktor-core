package com.aktor.core.model;

import java.util.Objects;
import java.util.ServiceLoader;

public final class RouterProvider
extends Provider<RouterFactory>
{
    public RouterProvider(final Configuration configuration, final Object... dependencies)
    {
        super(
            Objects.requireNonNull(configuration),
            RouterFactory.class,
            ServiceLoader.load(RouterFactoryLoader.class),
            dependencies
        );
    }

    public static RouterProvider of(final Configuration configuration, final Object... dependencies)
    {
        return new RouterProvider(configuration, dependencies);
    }

    public Router router(final String name)
    {
        return super.instance(
            Objects.requireNonNull(name),
            (safeName, context, route) -> RouterFactory.of(this, safeName).create(context, route)
        );
    }
}
