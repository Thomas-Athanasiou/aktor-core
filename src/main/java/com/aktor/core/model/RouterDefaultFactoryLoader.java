package com.aktor.core.model;

public final class RouterDefaultFactoryLoader
implements RouterFactoryLoader
{
    @Override
    public String kind()
    {
        return "default";
    }

    @Override
    public RouterFactory load(final Environment environment)
    {
        return new RouterDefaultFactory(environment.require(EndpointProvider.class));
    }
}
