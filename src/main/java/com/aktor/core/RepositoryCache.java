package com.aktor.core;

import com.aktor.core.exception.GetException;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.FactoryContext;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryFactoryLoader;
import com.aktor.core.model.RepositoryRequest;
import com.aktor.core.util.CompositeAccessUtil;
import com.aktor.core.util.CompositeSearchUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RepositoryCache<Item extends Data<Key>, Key>
extends RepositoryComposite<Item, Key>
{
    private final CacheWritePolicy cacheWritePolicy;

    public RepositoryCache(final List<Repository<Item, Key>> repositoryList)
    {
        this(repositoryList, CacheWritePolicy.ALL);
    }

    public RepositoryCache(final List<Repository<Item, Key>> repositoryList, final int cacheWriteSourceCount)
    {
        this(repositoryList, CacheWritePolicy.firstN(cacheWriteSourceCount));
    }

    public RepositoryCache(final List<Repository<Item, Key>> repositoryList, final CacheWritePolicy cacheWritePolicy)
    {
        super(repositoryList);
        this.cacheWritePolicy = Objects.requireNonNull(cacheWritePolicy);
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        return CompositeAccessUtil.getCache(
            getRepositories(),
            key,
            cacheWritePolicy.writeSourceCount(),
            Repository::get,
            Repository::save
        );
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return CompositeSearchUtil.searchCache(
            getRepositories(),
            CompositeAccessUtil.requireSearchCriteria(searchCriteria),
            Repository::search,
            Repository::save,
            cacheWritePolicy
        );
    }

    @Override
    public synchronized void save(final Item item) throws SaveException
    {
        final List<Repository<Item, Key>> repositories = getRepositories();
        final int limit = Math.min(cacheWritePolicy.writeSourceCount(), repositories.size());
        if (limit < 1)
        {
            return;
        }
        try
        {
            for (int index = 0; index < limit; index++)
            {
                repositories.get(index).save(item);
            }
        }
        catch (final Exception exception)
        {
            if (exception instanceof final SaveException saveException)
            {
                throw saveException;
            }
            throw new SaveException(exception);
        }
    }

    @Override
    public synchronized void delete(final Item item) throws DeleteException
    {
        final List<Repository<Item, Key>> repositories = getRepositories();
        final int limit = Math.min(cacheWritePolicy.writeSourceCount(), repositories.size());
        if (limit < 1)
        {
            return;
        }
        try
        {
            for (int index = 0; index < limit; index++)
            {
                repositories.get(index).delete(item);
            }
        }
        catch (final Exception exception)
        {
            if (exception instanceof final DeleteException deleteException)
            {
                throw deleteException;
            }
            throw new DeleteException(exception);
        }
    }

    public static final class Factory
    implements RepositoryFactory
    {
        @Override
        public <Item extends Data<Key>, Key> Repository<Item, Key> create(
            final FactoryContext context,
            final RepositoryRequest<Item, Key> request
        )
        {
            final RepositoryProvider provider = RepositoryFactory.requireProvider(context);
            final Configuration cache = cache(provider.configuration(), request.name());
            final Configuration sources = cache.getConfiguration("sources");
            final List<Repository<Item, Key>> repositories = new ArrayList<>();
            for (final String sourceName : sources.keys())
            {
                repositories.add(
                    provider.repository(
                        sourceName,
                        request.itemType(),
                        request.keyType(),
                        request.relationProviderResolver()
                    )
                );
            }
            return new RepositoryCache<>(repositories, cacheWriteSourceCount(cache));
        }

        private static Configuration cache(final Configuration configuration, final String name)
        {
            final Configuration entities = configuration.getConfiguration("entity");
            if (entities.has(name))
            {
                return entities.getConfiguration(name).getConfiguration("aggregate");
            }
            return configuration.getConfiguration(name).getConfiguration("aggregate");
        }

        private static int cacheWriteSourceCount(final Configuration cache)
        {
            final String value = cache.getString("cacheWriteSourceCount");
            if (value == null || value.isBlank())
            {
                return 1;
            }
            try
            {
                return Math.max(0, Integer.parseInt(value.trim()));
            }
            catch (final NumberFormatException exception)
            {
                throw new IllegalArgumentException("Invalid cacheWriteSourceCount: " + value, exception);
            }
        }
    }

    public static final class Loader
    implements RepositoryFactoryLoader
    {
        @Override
        public String kind()
        {
            return "cache";
        }

        @Override
        public RepositoryFactory load(final Environment environment)
        {
            return new Factory();
        }
    }
}
