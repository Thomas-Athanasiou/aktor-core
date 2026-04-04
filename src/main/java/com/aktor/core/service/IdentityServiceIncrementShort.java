package com.aktor.core.service;

public final class IdentityServiceIncrementShort
extends IdentityServiceIncrement<Short>
{
    public IdentityServiceIncrementShort()
    {
        this((short) 1);
    }

    public IdentityServiceIncrementShort(final short initialValue)
    {
        super(initialValue);
    }

    @Override
    protected Short increment(final Short current)
    {
        return (short) (current + 1);
    }
}
