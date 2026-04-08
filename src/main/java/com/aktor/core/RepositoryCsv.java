package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.CollectionProcessor;
import com.aktor.core.model.SearchCriteriaCondition;
import com.aktor.core.util.CsvTableUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RepositoryCsv<Item extends Data<Key>, Key>
implements Repository<Item, Key>
{
    private final String keyField;
    private final Converter<Item, Row> serializer;
    private final Converter<Map<String, String>, Item> deserializer;
    private final CollectionProcessor<Item, Key> processor;
    private final CsvReader reader;
    private final CsvWriter writer;
    private volatile CsvTableUtil.CsvTable cachedTable;

    public RepositoryCsv(
        final String keyField,
        final Converter<Item, Row> serializer,
        final Converter<Map<String, String>, Item> deserializer,
        final CsvReader reader,
        final CsvWriter writer
    )
    {
        this(
            keyField,
            serializer,
            deserializer,
            new CollectionProcessor<>(new SearchCriteriaCondition(), serializer),
            reader,
            writer
        );
    }

    public RepositoryCsv(
        final String keyField,
        final Converter<Item, Row> serializer,
        final Converter<Map<String, String>, Item> deserializer,
        final CollectionProcessor<Item, Key> processor,
        final CsvReader reader,
        final CsvWriter writer
    )
    {
        super();
        this.keyField = Objects.requireNonNull(keyField);
        this.serializer = Objects.requireNonNull(serializer);
        this.deserializer = Objects.requireNonNull(deserializer);
        this.processor = Objects.requireNonNull(processor);
        this.reader = Objects.requireNonNull(reader);
        this.writer = writer;
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        try
        {
            final String keyValue = String.valueOf(key);
            for (final Map<String, String> row : loadTable().rows())
            {
                if (Objects.equals(Row.get(row, keyField), keyValue))
                {
                    return deserializer.convert(row);
                }
            }
            throw new GetException(
                "The item with key " + key + " that was requested doesn't exist, verify the item and try again"
            );
        }
        catch (final GetException exception)
        {
            throw exception;
        }
        catch (final Exception exception)
        {
            throw new GetException(exception);
        }
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        try
        {
            return processor.process(loadItems(), searchCriteria);
        }
        catch (final SearchException exception)
        {
            throw exception;
        }
        catch (final Exception exception)
        {
            throw new SearchException(exception);
        }
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        try
        {
            ensureWritable();
            final CsvTableUtil.CsvTable csvTable = loadTable();
            final List<String> headers = new ArrayList<>(csvTable.headers());
            final Map<String, String> row = toFieldMap(item);
            ensureHeaders(headers, row);
            final List<Map<String, String>> rows = copyRows(csvTable.rows());
            final String keyValue = String.valueOf(item.key());
            final int existingIndex = findRowIndex(rows, keyValue);
            final Map<String, String> normalizedRow = normalizeRow(headers, row);
            if (existingIndex < 0)
            {
                rows.add(normalizedRow);
            }
            else
            {
                rows.set(existingIndex, normalizedRow);
            }
            persist(new CsvTableUtil.CsvTable(headers, rows));
        }
        catch (final Exception exception)
        {
            throw new SaveException(exception);
        }
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        try
        {
            ensureWritable();
            final CsvTableUtil.CsvTable csvTable = loadTable();
            final List<Map<String, String>> rows = copyRows(csvTable.rows());
            final int existingIndex = findRowIndex(rows, String.valueOf(item.key()));
            if (existingIndex >= 0)
            {
                rows.remove(existingIndex);
                persist(new CsvTableUtil.CsvTable(csvTable.headers(), rows));
            }
        }
        catch (final Exception exception)
        {
            throw new DeleteException(exception);
        }
    }

    private List<Item> loadItems() throws ConversionException
    {
        final List<Item> items = new ArrayList<>();
        for (final Map<String, String> row : loadTable().rows())
        {
            items.add(deserializer.convert(row));
        }
        return items;
    }

    private CsvTableUtil.CsvTable loadTable()
    {
        CsvTableUtil.CsvTable current = cachedTable;
        if (current != null)
        {
            return current;
        }
        synchronized (this)
        {
            current = cachedTable;
            if (current == null)
            {
                current = CsvTableUtil.parse(reader.read());
                cachedTable = current;
            }
            return current;
        }
    }

    private void persist(final CsvTableUtil.CsvTable csvTable) throws Exception
    {
        writer.write(CsvTableUtil.serialize(csvTable));
        cachedTable = csvTable;
    }

    private void ensureWritable()
    {
        if (writer == null)
        {
            throw new IllegalStateException("CSV repository does not support writes.");
        }
    }

    private Map<String, String> toFieldMap(final Item item) throws ConversionException
    {
        final Map<String, String> fieldMap = new LinkedHashMap<>();
        for (final Value value : serializer.convert(item).values())
        {
            if (value != null && value.field() != null)
            {
                fieldMap.putIfAbsent(value.field(), value.value());
            }
        }
        return fieldMap;
    }

    private void ensureHeaders(final List<String> headers, final Map<String, String> row)
    {
        for (final String field : row.keySet())
        {
            if (!containsField(headers, field))
            {
                headers.add(field);
            }
        }
        if (!containsField(headers, keyField))
        {
            headers.add(0, keyField);
        }
    }

    private int findRowIndex(final List<Map<String, String>> rows, final String keyValue)
    {
        for (int index = 0; index < rows.size(); index++)
        {
            if (Objects.equals(Row.get(rows.get(index), keyField), keyValue))
            {
                return index;
            }
        }
        return -1;
    }

    private static List<Map<String, String>> copyRows(final List<Map<String, String>> rows)
    {
        final List<Map<String, String>> result = new ArrayList<>(rows.size());
        for (final Map<String, String> row : rows)
        {
            result.add(new LinkedHashMap<>(row));
        }
        return result;
    }

    private static Map<String, String> normalizeRow(
        final List<String> headers,
        final Map<String, String> row
    )
    {
        final Map<String, String> result = new LinkedHashMap<>(headers.size());
        for (final String header : headers)
        {
            result.put(header, Row.get(row, header));
        }
        return result;
    }

    private static boolean containsField(final Iterable<String> headers, final String field)
    {
        for (final String header : headers)
        {
            if (header != null && header.equalsIgnoreCase(field))
            {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface CsvReader
    {
        String read();
    }

    @FunctionalInterface
    public interface CsvWriter
    {
        void write(String csv) throws Exception;
    }
}
