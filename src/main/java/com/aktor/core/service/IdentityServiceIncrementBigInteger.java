package com.aktor.core.service;

import java.math.BigInteger;

public final class IdentityServiceIncrementBigInteger
extends IdentityServiceIncrement<BigInteger>
{
    public IdentityServiceIncrementBigInteger()
    {
        this(BigInteger.ONE);
    }

    public IdentityServiceIncrementBigInteger(final BigInteger initialValue)
    {
        super(initialValue);
    }

    @Override
    protected BigInteger increment(final BigInteger current)
    {
        return current.add(BigInteger.ONE);
    }
}
