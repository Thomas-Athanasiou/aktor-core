package com.aktor.core;

import com.aktor.core.exception.SearchException;
import com.aktor.core.model.CollectionProcessor;

import java.util.Objects;

public abstract class SearchExecutor<Item extends Data<Key>, Key>
implements Search<Item, Key>
{
    private final CollectionProcessor<Item, Key> processor;

    protected SearchExecutor(
        final CollectionProcessor<Item, Key> processor
    )
    {
        this.processor = Objects.requireNonNull(processor);
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return search(searchCriteria, source());
    }

    protected final SearchResult<Item> search(final SearchCriteria searchCriteria, final SearchSource<Item, Key> source)
    throws SearchException
    {
        return processor.process(Objects.requireNonNull(source).items(), searchCriteria);
    }

    protected abstract SearchSource<Item, Key> source() throws SearchException;
}
