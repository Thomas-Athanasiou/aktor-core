package com.aktor.core.service;

public final class IdentityServiceIncrementByte
extends IdentityServiceIncrement<Byte>
{
    public IdentityServiceIncrementByte()
    {
        this((byte) 1);
    }

    public IdentityServiceIncrementByte(final byte initialValue)
    {
        super(initialValue);
    }

    @Override
    protected Byte increment(final Byte current)
    {
        return (byte) (current + 1);
    }
}
