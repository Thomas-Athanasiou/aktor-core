package com.aktor.core.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EnvironmentDefault
implements Environment
{
    private record LoaderKey(Class<?> type, String kind)
    {
        private LoaderKey
        {
            type = Objects.requireNonNull(type);
            kind = Objects.requireNonNull(kind);
            if (kind.isBlank())
            {
                throw new IllegalArgumentException("kind cannot be blank");
            }
        }
    }

    private final List<Object> services = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<LoaderKey, Loader<?>> loaders = new ConcurrentHashMap<>();

    @Override
    public <Type> void put(final Type service)
    {
        services.add(Objects.requireNonNull(service));
    }

    @Override
    public <Type> Type get(final Class<Type> type)
    {
        final Class<Type> safeType = Objects.requireNonNull(type);
        for (final Object service : services)
        {
            if (safeType.isInstance(service))
            {
                return safeType.cast(service);
            }
        }
        return null;
    }

    @Override
    public <Type> Type require(final Class<Type> type)
    {
        final Type value = get(type);
        if (value == null)
        {
            throw new IllegalArgumentException("No service available for type: " + type.getName());
        }
        return value;
    }

    @Override
    public <Type> void registerLoader(final Class<Type> type, final Loader<? extends Type> loader)
    {
        loaders.put(new LoaderKey(type, Objects.requireNonNull(loader).kind()), loader);
    }

    @Override
    public <Type> Type load(final Class<Type> type, final String kind)
    {
        final Loader<?> loader = loaders.get(new LoaderKey(type, kind));
        if (loader == null)
        {
            throw new IllegalArgumentException(
                "No loader registered for type " + type.getName() + " and kind '" + kind + "'"
            );
        }
        return type.cast(loader.load(this));
    }
}
