package com.aktor.core;

import com.aktor.core.exception.ConversionException;

public final class DataRowSerializerProperties
implements Converter<DataRow, String>
{
    @Override
    public String convert(final DataRow dataRow) throws ConversionException
    {
        return PropertiesDataRowCodec.serialize(dataRow);
    }
}
