package com.aktor.core;

import com.aktor.core.exception.ConversionException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

final class PropertiesDataRowCodec
{
    private static final Value[] VALUES = new Value[0];

    private PropertiesDataRowCodec()
    {
        super();
    }

    static DataRow deserialize(final String input) throws ConversionException
    {
        try
        {
            final Properties properties = new Properties();
            properties.load(new StringReader(input));
            final List<Value> values = new ArrayList<>(properties.size());
            for (final var entry : properties.entrySet())
            {
                values.add(new Value(entry.getKey().toString(), entry.getValue().toString()));
            }
            return DataRow.of(values.toArray(VALUES));
        }
        catch (final IOException exception)
        {
            throw new ConversionException(exception);
        }
    }

    static String serialize(final DataRow dataRow) throws ConversionException
    {
        final StringWriter writer = new StringWriter();
        try
        {
            final Properties properties = new Properties();
            final Value[] values = dataRow.values();
            for (final Value value : values)
            {
                properties.setProperty(value.field(), value.value());
            }
            properties.store(writer, null);
        }
        catch (final IOException exception)
        {
            throw new ConversionException(exception);
        }
        return writer.toString();
    }
}
