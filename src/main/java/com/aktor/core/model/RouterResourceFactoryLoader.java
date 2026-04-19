package com.aktor.core.model;

public final class RouterResourceFactoryLoader
implements RouterFactoryLoader
{
    @Override
    public String kind()
    {
        return "resource";
    }

    @Override
    public RouterFactory load(final Environment environment)
    {
        return new RouterResourceFactory(environment.require(EndpointProvider.class));
    }
}
