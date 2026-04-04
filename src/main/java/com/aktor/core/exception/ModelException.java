package com.aktor.core.exception;

public final class ModelException
extends Exception
{
    public ModelException(final String message)
    {
        super(message);
    }

    public ModelException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public ModelException(final Throwable cause)
    {
        super(cause);
    }

    public String getRootCauseMessage()
    {
        return getRootCause().getMessage();
    }

    private Throwable getRootCause()
    {
        Throwable cause;
        Throwable result = this;
        while(null != (cause = result.getCause())  && (result != cause))
        {
            result = cause;
        }
        return result;
    }

}
