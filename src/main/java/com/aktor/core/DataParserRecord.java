package com.aktor.core;

import com.aktor.core.exception.ConversionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataParserRecord<Item extends java.lang.Record & Data<?>>
implements Converter<DataRow, Item>
{
    private final Mapper<Item, ?> mapper;

    public DataParserRecord(final Mapper<Item, ?> mapper)
    {
        super();
        this.mapper = Objects.requireNonNull(mapper);
    }

    public Item convert(final DataRow dataRow) throws ConversionException
    {
        final Value[] values = dataRow.values();
        final Map<String, String> map = new HashMap<>(Math.max(4, values.length * 2));
        for (final Value value : values)
        {
            if (value != null)
            {
                map.put(value.field(), value.value());
            }
        }
        return mapper.convert(map);
    }
}
