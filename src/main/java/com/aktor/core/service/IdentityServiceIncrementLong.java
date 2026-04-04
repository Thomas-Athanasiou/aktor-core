package com.aktor.core.service;

public final class IdentityServiceIncrementLong
extends IdentityServiceIncrement<Long>
{
    public IdentityServiceIncrementLong()
    {
        this(1L);
    }

    public IdentityServiceIncrementLong(final long initialValue)
    {
        super(initialValue);
    }

    @Override
    protected Long increment(final Long current)
    {
        return current + 1L;
    }
}
