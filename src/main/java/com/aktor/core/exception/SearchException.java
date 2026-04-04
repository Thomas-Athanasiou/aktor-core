package com.aktor.core.exception;

public final class SearchException
extends Exception
{
    public SearchException(final String message)
    {
        super(message);
    }

    public SearchException(final Throwable cause)
    {
        super(cause);
    }

    public SearchException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
