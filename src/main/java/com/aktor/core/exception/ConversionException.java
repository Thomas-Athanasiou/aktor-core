package com.aktor.core.exception;

public final class ConversionException
extends Exception
{
    public ConversionException(final String message)
    {
        super(message);
    }

    public ConversionException(final Exception exception)
    {
        super(exception);
    }

    public ConversionException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
