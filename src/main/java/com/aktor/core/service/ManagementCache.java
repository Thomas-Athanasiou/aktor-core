package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.CacheWritePolicy;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.util.CompositeAccessUtil;
import com.aktor.core.util.CompositeSearchUtil;

import java.util.List;
import java.util.Objects;

public class ManagementCache<Item extends Data<Key>, Key>
extends ManagementComposite<Item, Key>
{
    private final CacheWritePolicy cacheWritePolicy;

    public ManagementCache(final List<Management<Item, Key>> managementList)
    {
        this(managementList, CacheWritePolicy.ALL);
    }

    public ManagementCache(final List<Management<Item, Key>> managementList, final int cacheWriteSourceCount)
    {
        this(managementList, CacheWritePolicy.firstN(cacheWriteSourceCount));
    }

    public ManagementCache(final List<Management<Item, Key>> managementList, final CacheWritePolicy cacheWritePolicy)
    {
        super(managementList);
        this.cacheWritePolicy = Objects.requireNonNull(cacheWritePolicy);
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        return CompositeAccessUtil.getCache(
            managementList,
            key,
            cacheWritePolicy.writeSourceCount(),
            Management::get,
            Management::save
        );
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return CompositeSearchUtil.searchCache(
            managementList,
            CompositeAccessUtil.requireSearchCriteria(searchCriteria),
            Management::search,
            Management::save,
            cacheWritePolicy
        );
    }
}
