package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.util.CompositeAccessUtil;
import com.aktor.core.util.CompositeSearchUtil;

import java.util.List;

public class ManagementAggregate<Item extends Data<Key>, Key>
extends ManagementComposite<Item, Key>
{
    public ManagementAggregate(final List<Management<Item, Key>> managementList)
    {
        super(managementList);
    }

    @Override
    public final Item get(final Key key) throws GetException
    {
        return CompositeAccessUtil.getAggregate(managementList, key, Management::get);
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return CompositeSearchUtil.searchAggregate(
            managementList,
            CompositeAccessUtil.requireSearchCriteria(searchCriteria),
            Management::search
        );
    }
}
