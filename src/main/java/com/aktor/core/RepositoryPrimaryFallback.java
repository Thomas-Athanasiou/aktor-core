package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.FactoryContext;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryFactoryLoader;
import com.aktor.core.model.RepositoryProvider;
import com.aktor.core.model.RepositoryRequest;
import com.aktor.core.util.CompositeAccessUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RepositoryPrimaryFallback<Item extends Data<Key>, Key>
extends RepositoryComposite<Item, Key>
{
    public RepositoryPrimaryFallback(final List<Repository<Item, Key>> repositoryList)
    {
        super(repositoryList);
        if (repositoryList.isEmpty())
        {
            throw new IllegalArgumentException("At least one fallback repository source is required.");
        }
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        return CompositeAccessUtil.getAggregate(getRepositories(), key, Repository::get);
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        final Map<Key, Item> items = new LinkedHashMap<>();
        SearchCriteria criteria = searchCriteria;
        for (final Repository<Item, Key> repository : getRepositories())
        {
            final SearchResult<Item> result = repository.search(searchCriteria);
            criteria = result.searchCriteria();
            for (final Item item : result.items())
            {
                items.putIfAbsent(item.key(), item);
            }
        }
        return new SearchResult<>(List.copyOf(items.values()), criteria, items.size());
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        getRepositories().get(0).save(item);
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        getRepositories().get(0).delete(item);
    }

    public static final class Factory
    implements RepositoryFactory
    {
        @Override
        public <Item extends Data<Key>, Key> Repository<Item, Key> createTyped(
            final FactoryContext context,
            final RepositoryRequest<Item, Key> request
        )
        {
            final RepositoryProvider provider = RepositoryFactory.requireProvider(context);
            final Configuration fallback = fallback(provider.configuration(), request.name());
            final Configuration sources = fallback.getConfiguration("sources");
            final List<Repository<Item, Key>> repositories = new ArrayList<>();
            for (final String sourceName : sources.keys())
            {
                repositories.add(provider.instance(
                    sourceName,
                    request.itemType(),
                    request.keyType(),
                    request.relationProviderResolver()
                ));
            }
            return new RepositoryPrimaryFallback<>(repositories);
        }

        private static Configuration fallback(final Configuration configuration, final String name)
        {
            final Configuration entities = configuration.getConfiguration("entity");
            if (entities.has(name))
            {
                final Configuration entity = entities.getConfiguration(name);
                if (entity.has("fallback"))
                {
                    return entity.getConfiguration("fallback");
                }
                return entity.getConfiguration("aggregate");
            }
            final Configuration entity = configuration.getConfiguration(name);
            if (entity.has("fallback"))
            {
                return entity.getConfiguration("fallback");
            }
            return entity.getConfiguration("aggregate");
        }
    }

    public static final class Loader
    implements RepositoryFactoryLoader
    {
        @Override
        public String kind()
        {
            return "fallback";
        }

        @Override
        public RepositoryFactory load(final Environment environment)
        {
            return new Factory();
        }
    }
}
