package com.aktor.core;

import com.aktor.core.exception.SearchException;

public interface Search<Item extends Data<Key>, Key>
{
    SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException;
}
