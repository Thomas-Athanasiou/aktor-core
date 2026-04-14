package com.aktor.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class Provider<Factory>
implements FactoryContext
{
    private final Configuration configuration;

    private final Environment environment;

    protected Provider(
        final Configuration configuration,
        final Class<Factory> factoryClass,
        final Iterable<? extends Loader<? extends Factory>> loaders,
        final Object... dependencies
    )
    {
        this.configuration = Objects.requireNonNull(configuration);
        this.environment = new EnvironmentDefault();
        environment.put(this);
        if (dependencies != null)
        {
            for (final Object dependency : dependencies)
            {
                environment.put(dependency);
            }
        }
        if (loaders != null)
        {
            for (final Loader<? extends Factory> loader : loaders)
            {
                if (loader != null)
                {
                    environment.registerLoader(factoryClass, loader);
                }
            }
        }
    }

    @SafeVarargs
    protected static <Type> Iterable<Loader<? extends Type>> combineLoaders(
        final Iterable<? extends Loader<? extends Type>> discovered,
        final Loader<? extends Type>... fixed
    )
    {
        final Collection<Loader<? extends Type>> result = new ArrayList<>();
        if (fixed != null)
        {
            for (final Loader<? extends Type> loader : fixed)
            {
                if (loader != null)
                {
                    result.add(loader);
                }
            }
        }
        if (discovered != null)
        {
            for (final Loader<? extends Type> loader : discovered)
            {
                if (loader != null)
                {
                    result.add(loader);
                }
            }
        }
        return result;
    }

    @Override
    public Configuration configuration()
    {
        return configuration;
    }

    @Override
    public Environment environment()
    {
        return environment;
    }

    @FunctionalInterface
    public interface FactoryInvoker<Request, Instance>
    {
        Instance create(String name, FactoryContext context, Request request);
    }

    protected <Instance> Instance instance(final String name, final FactoryInvoker<String, Instance> invoker)
    {
        return instance(name, name, invoker);
    }

    protected <Request, Instance> Instance instance(
        final String name,
        final Request request,
        final FactoryInvoker<Request, Instance> invoker
    )
    {
        return Objects.requireNonNull(Objects.requireNonNull(invoker).create(Objects.requireNonNull(name), this, request));
    }
}
