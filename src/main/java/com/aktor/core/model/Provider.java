package com.aktor.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Provider<FactoryType, InstanceType>
implements FactoryContext
{
    private final Configuration configuration;

    private final Environment environment;

    private final Map<String, InstanceType> cache = new LinkedHashMap<>();

    protected Provider(
        final Configuration configuration,
        final Class<FactoryType> factoryClass,
        final Iterable<? extends Loader<? extends FactoryType>> loaders,
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
            for (final Loader<? extends FactoryType> loader : loaders)
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

    protected InstanceType instance(
        final String name,
        final Function<String, FactoryType> factoryResolver,
        final BiFunction<FactoryType, String, InstanceType> instanceCreator
    )
    {
        final String safeName = Objects.requireNonNull(name);
        final InstanceType existing = cache.get(safeName);
        if (existing != null)
        {
            return existing;
        }
        final FactoryType factory = Objects.requireNonNull(factoryResolver.apply(safeName));
        final InstanceType created = Objects.requireNonNull(instanceCreator.apply(factory, safeName));
        cache.put(safeName, created);
        return created;
    }

    protected <Request> InstanceType instance(
        final String name,
        final Request request,
        final Function<String, FactoryType> factoryResolver,
        final BiFunction<FactoryType, Request, InstanceType> instanceCreator
    )
    {
        final String safeName = Objects.requireNonNull(name);
        final InstanceType existing = cache.get(safeName);
        if (existing != null)
        {
            return existing;
        }
        final FactoryType factory = Objects.requireNonNull(factoryResolver.apply(safeName));
        final InstanceType created = Objects.requireNonNull(instanceCreator.apply(factory, request));
        cache.put(safeName, created);
        return created;
    }
}
