package com.aktor.core.service;

public final class IdentityServiceIncrementInteger
extends IdentityServiceIncrement<Integer>
{
    public IdentityServiceIncrementInteger()
    {
        this(1);
    }

    public IdentityServiceIncrementInteger(final int initialValue)
    {
        super(initialValue);
    }

    @Override
    protected Integer increment(final Integer current)
    {
        return current + 1;
    }
}
