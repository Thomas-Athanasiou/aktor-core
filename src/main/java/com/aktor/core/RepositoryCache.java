package com.aktor.core;

import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.util.CompositeAccessUtil;
import com.aktor.core.util.CompositeSearchUtil;

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
}
