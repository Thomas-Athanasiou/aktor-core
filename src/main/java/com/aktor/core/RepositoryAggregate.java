package com.aktor.core;

import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryFactoryLoader;
import com.aktor.core.model.RepositoryProvider;
import com.aktor.core.util.CompositeAccessUtil;
import com.aktor.core.util.CompositeSearchUtil;

import java.util.ArrayList;
import java.util.List;

public class RepositoryAggregate<Item extends Data<Key>, Key>
extends RepositoryComposite<Item, Key>
{
    public RepositoryAggregate(final List<Repository<Item, Key>> repositoryList)
    {
        super(repositoryList);
    }

    @Override
    public final Item get(final Key key) throws GetException
    {
        return CompositeAccessUtil.getAggregate(getRepositories(), key, Repository::get);
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return CompositeSearchUtil.searchAggregate(
            getRepositories(),
            CompositeAccessUtil.requireSearchCriteria(searchCriteria),
            Repository::search
        );
    }

    public static final class Factory implements RepositoryFactory
    {
        @Override
        public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
            final RepositoryProvider provider,
            final String name,
            final Class<Item> itemType,
            final Class<Key> keyType
        )
        {
            return repository(provider, name, itemType, keyType, new com.aktor.core.model.RelationProviderResolver<>());
        }

        @Override
        public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
            final RepositoryProvider provider,
            final String name,
            final Class<Item> itemType,
            final Class<Key> keyType,
            final com.aktor.core.model.RelationProviderResolver<Key> relationProviderResolver
        )
        {
            final Configuration aggregate = aggregate(provider.configuration(), name);
            final Configuration sources = aggregate.getConfiguration("sources");
            final List<Repository<Item, Key>> repositories = new ArrayList<>();
            for (final String sourceName : sources.keys())
            {
                repositories.add(provider.repository(sourceName, itemType, keyType, relationProviderResolver));
            }
            return new RepositoryAggregate<>(repositories);
        }

        private static Configuration aggregate(final Configuration configuration, final String name)
        {
            final Configuration entities = configuration.getConfiguration("entity");
            if (entities.has(name))
            {
                return entities.getConfiguration(name).getConfiguration("aggregate");
            }
            return configuration.getConfiguration(name).getConfiguration("aggregate");
        }
    }

    public static final class Loader
    implements RepositoryFactoryLoader
    {
        @Override
        public String kind()
        {
            return "aggregate";
        }

        @Override
        public RepositoryFactory load(final Environment environment)
        {
            return new Factory();
        }
    }
}
