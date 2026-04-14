package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.RepositoryAggregate;
import com.aktor.core.RepositoryCache;
import com.aktor.core.RepositoryReadOnly;
import com.aktor.core.RepositoryRotating;

import java.util.Objects;
import java.util.ServiceLoader;

public final class RepositoryProvider<Item extends Data<Key>, Key>
extends Provider<RepositoryFactory<Item, Key>>
{
    public RepositoryProvider(final Configuration configuration, final Object... dependencies)
    {
        super(
            Objects.requireNonNull(configuration),
            factoryClass(),
            loaders(),
            dependencies
        );
    }

    public static <Item extends Data<Key>, Key> RepositoryProvider<Item, Key> of(
        final Configuration configuration,
        final Object... dependencies
    )
    {
        return new RepositoryProvider<>(configuration, dependencies);
    }

    public <Dependency> Dependency require(final Class<Dependency> type)
    {
        return super.environment().require(Objects.requireNonNull(type));
    }

    public Repository<Item, Key> instance(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        return instance(name, itemType, keyType, new RelationProviderResolver<>());
    }

    public Repository<Item, Key> instance(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        final RepositoryRequest<Item, Key> request = RepositoryFactory.request(
            Objects.requireNonNull(name),
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(keyType),
            Objects.requireNonNull(relationProviderResolver)
        );
        return super.instance(
            request.name(),
            request,
            this::createRepository
        );
    }

    public Repository<Item, Key> instance(final RepositoryRequest<Item, Key> request)
    {
        final RepositoryRequest<Item, Key> safeRequest = Objects.requireNonNull(request);
        return super.instance(
            safeRequest.name(),
            safeRequest,
            this::createRepository
        );
    }

    private Repository<Item, Key> createRepository(
        final String name,
        final FactoryContext context,
        final RepositoryRequest<Item, Key> request
    )
    {
        return RepositoryFactory.of(this, name).create(context, request);
    }

    @SuppressWarnings("unchecked")
    private static <Item extends Data<Key>, Key> Class<RepositoryFactory<Item, Key>> factoryClass()
    {
        return (Class<RepositoryFactory<Item, Key>>) (Class<?>) RepositoryFactory.class;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <Item extends Data<Key>, Key> Iterable<? extends Loader<? extends RepositoryFactory<Item, Key>>> loaders()
    {
        final Iterable<? extends Loader<? extends RepositoryFactory<Item, Key>>> discovered = (Iterable) ServiceLoader.load(
            RepositoryFactoryLoader.class
        );
        return combineLoaders(
             discovered,
             new RepositoryAggregate.Loader<>(),
             new RepositoryCache.Loader<>(),
             new RepositoryReadOnly.Loader<>(),
             new RepositoryRotating.Loader<>()
         );
    }
}
