package com.aktor.core;

import com.aktor.core.exception.ConversionException;

public final class DataRowDeserializerProperties
implements Converter<String, DataRow>
{
    @Override
    public DataRow convert(final String input) throws ConversionException
    {
        return PropertiesDataRowCodec.deserialize(input);
    }
}
