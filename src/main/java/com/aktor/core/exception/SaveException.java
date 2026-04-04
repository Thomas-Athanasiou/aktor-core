package com.aktor.core.exception;

public final class SaveException
extends Exception
{
    public SaveException(final String message)
    {
        super(message);
    }

    public SaveException(final Throwable cause)
    {
        super(cause);
    }

    public SaveException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
