package com.aktor.core.model;

public final class RouterCompositeFactoryLoader
implements RouterFactoryLoader
{
    @Override
    public String kind()
    {
        return "composite";
    }

    @Override
    public RouterFactory load(final Environment environment)
    {
        return new RouterCompositeFactory(environment.require(RouterProvider.class));
    }
}
