package com.aktor.core;

import com.aktor.core.exception.SearchException;

@FunctionalInterface
public interface SearchSource<Item extends Data<Key>, Key>
{
    Iterable<Item> items() throws SearchException;
}
