package com.aktor.core;

import com.aktor.core.exception.ConversionException;

public final class DataRowSerializerProperties
implements Converter<Row, String>
{
    @Override
    public String convert(final Row row) throws ConversionException
    {
        return PropertiesDataRowCodec.serialize(row);
    }
}
