package com.aktor.core.exception;

public final class GetException
extends Exception
{
    public GetException(final String message)
    {
        super(message);
    }

    public GetException(final Throwable cause)
    {
        super(cause);
    }

    public GetException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
