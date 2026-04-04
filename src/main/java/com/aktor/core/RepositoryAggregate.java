package com.aktor.core;

import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.util.CompositeAccessUtil;
import com.aktor.core.util.CompositeSearchUtil;

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
}
