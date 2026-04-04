package com.aktor.core.exception;

public final class DeleteException
extends Exception
{
    public DeleteException(final String message)
    {
        super(message);
    }

    public DeleteException(final Throwable cause)
    {
        super(cause);
    }

    public DeleteException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}