package com.aktor.core.model;

import java.util.Objects;

public final class RouterCompositeFactory
implements RouterFactory
{
    public static final String CONFIG_ROUTE = "route";
    public static final String CONFIG_PATH = "path";
    public static final String CONFIG_ROUTER = "router";

    private final RouterProvider provider;

    public RouterCompositeFactory(final RouterProvider provider)
    {
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public Router create(final FactoryContext context, final String name)
    {
        final Configuration routerConfig = RouterFactory.config(provider, name);
        final Configuration routesConfig = routerConfig.getConfiguration(CONFIG_ROUTE);
        final RouterComposite composite = new RouterComposite();
        final String[] routeKeys = routesConfig.keys();
        if (routeKeys.length > 0)
        {
            for (final String routeKey : routeKeys)
            {
                registerRoute(composite, routesConfig.getConfiguration(routeKey));
            }
        }
        else
        {
            for (final String key : routerConfig.keys())
            {
                if (key == null || key.isBlank())
                {
                    continue;
                }
                if (FactoryConfig.CONFIG_KIND.equalsIgnoreCase(key)
                    || FactoryConfig.CONFIG_TYPE.equalsIgnoreCase(key))
                {
                    continue;
                }
                registerRoute(composite, routerConfig.getConfiguration(key));
            }
        }
        return composite;
    }

    private void registerRoute(final RouterComposite composite, final Configuration routeConfig)
    {
        Objects.requireNonNull(composite);
        if (routeConfig == null)
        {
            return;
        }
        final String path = routeConfig.getString(CONFIG_PATH);
        final String routerName = routeConfig.getString(CONFIG_ROUTER);
        if (path == null || path.isBlank() || routerName == null || routerName.isBlank())
        {
            return;
        }
        composite.on(path.trim(), provider.router(routerName.trim()));
    }
}
