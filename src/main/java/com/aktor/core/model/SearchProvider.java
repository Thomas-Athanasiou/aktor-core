package com.aktor.core.model;

import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.exception.SearchException;

@FunctionalInterface
public interface SearchProvider<Source, Item>
{
    SearchResult<Item> search(Source source, SearchCriteria searchCriteria) throws SearchException;
}
