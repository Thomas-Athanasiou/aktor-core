package com.aktor.core;

import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.FactoryContext;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryFactoryLoader;
import com.aktor.core.model.RepositoryProvider;
import com.aktor.core.model.RepositoryRequest;
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

    public static final class Factory<Item extends Data<Key>, Key>
    implements RepositoryFactory<Item, Key>
    {
        @Override
        public Repository<Item, Key> create(
            final FactoryContext context,
            final RepositoryRequest<Item, Key> request
        )
        {
            final RepositoryProvider provider = RepositoryFactory.requireProvider(context);
            final Configuration aggregate = aggregate(provider.configuration(), request.name());
            final Configuration sources = aggregate.getConfiguration("sources");
            final List<Repository<Item, Key>> repositories = new ArrayList<>();
            for (final String sourceName : sources.keys())
            {
                repositories.add(
                    provider.instance(
                        sourceName,
                        request.itemType(),
                        request.keyType(),
                        request.relationProviderResolver()
                    )
                );
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

    public static final class Loader<Item extends Data<Key>, Key>
    implements RepositoryFactoryLoader<Item, Key>
    {
        @Override
        public String kind()
        {
            return "aggregate";
        }

        @Override
        public RepositoryFactory<Item, Key> load(final Environment environment)
        {
            return new Factory<>();
        }
    }
}
