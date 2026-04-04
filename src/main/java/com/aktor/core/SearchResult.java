package com.aktor.core;

import java.util.List;
import java.util.Objects;

public record SearchResult<Item>(List<Item> items, SearchCriteria searchCriteria, int totalCount)
{
    public SearchResult
    {
        Objects.requireNonNull(items);
        items = List.copyOf(items);
    }
}
