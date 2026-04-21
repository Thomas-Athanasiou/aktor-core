package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RepositoryConverted<Item extends Data<Key>, SourceItem extends Data<SourceKey>, Key, SourceKey>
implements Repository<Item, Key>
{
    public record ReadInput<SourceItem extends Data<SourceKey>, Key, SourceKey>(
        Key key,
        SourceKey sourceKey,
        SourceItem sourceItem
    )
    {
    }

    @FunctionalInterface
    public interface Reader<Item, SourceItem extends Data<SourceKey>, Key, SourceKey>
    {
        Item convert(ReadInput<SourceItem, Key, SourceKey> input) throws ConversionException;
    }

    private final Repository<SourceItem, SourceKey> source;
    private final Converter<Key, SourceKey> keyReader;
    private final Converter<SearchCriteria, SearchCriteria> searchReader;
    private final Reader<Item, SourceItem, Key, SourceKey> reader;
    private final Converter<Item, SourceItem> writer;

    public RepositoryConverted(
        final Repository<SourceItem, SourceKey> source,
        final Converter<Key, SourceKey> keyReader,
        final Converter<SourceItem, Item> reader,
        final Converter<Item, SourceItem> writer
    )
    {
        this(
            source,
            keyReader,
            searchCriteria -> searchCriteria,
            (Reader<Item, SourceItem, Key, SourceKey>) input -> reader.convert(input.sourceItem()),
            writer
        );
    }

    public RepositoryConverted(
        final Repository<SourceItem, SourceKey> source,
        final Converter<Key, SourceKey> keyReader,
        final Converter<SearchCriteria, SearchCriteria> searchReader,
        final Reader<Item, SourceItem, Key, SourceKey> reader,
        final Converter<Item, SourceItem> writer
    )
    {
        this.source = Objects.requireNonNull(source);
        this.keyReader = Objects.requireNonNull(keyReader);
        this.searchReader = Objects.requireNonNull(searchReader);
        this.reader = Objects.requireNonNull(reader);
        this.writer = writer;
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        try
        {
            final SourceKey sourceKey = keyReader.convert(key);
            return reader.convert(new ReadInput<>(key, sourceKey, source.get(sourceKey)));
        }
        catch (final ConversionException exception)
        {
            throw new GetException(exception);
        }
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        if (writer == null)
        {
            throw new SaveException("Converted repository is read-only.");
        }
        try
        {
            source.save(writer.convert(item));
        }
        catch (final ConversionException exception)
        {
            throw new SaveException(exception);
        }
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        if (writer == null)
        {
            throw new DeleteException("Converted repository is read-only.");
        }
        try
        {
            source.delete(writer.convert(item));
        }
        catch (final ConversionException exception)
        {
            throw new DeleteException(exception);
        }
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        try
        {
            final SearchResult<SourceItem> result = source.search(searchReader.convert(searchCriteria));
            final List<Item> items = new ArrayList<>();
            for (final SourceItem item : result.items())
            {
                items.add(reader.convert(new ReadInput<>(null, item.key(), item)));
            }
            return new SearchResult<>(items, searchCriteria, result.totalCount());
        }
        catch (final ConversionException exception)
        {
            throw new SearchException(exception);
        }
    }
}
