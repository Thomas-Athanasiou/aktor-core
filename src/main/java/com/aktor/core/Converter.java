package com.aktor.core;

import com.aktor.core.exception.ConversionException;

public interface Converter<From, To>
{
    To convert(From input) throws ConversionException;
}
