package com.aktor.core.service;

import java.math.BigDecimal;

public final class IdentityServiceIncrementBigDecimal
extends IdentityServiceIncrement<BigDecimal>
{
    public IdentityServiceIncrementBigDecimal()
    {
        this(BigDecimal.ONE);
    }

    public IdentityServiceIncrementBigDecimal(final BigDecimal initialValue)
    {
        super(initialValue);
    }

    @Override
    protected BigDecimal increment(final BigDecimal current)
    {
        return current.add(BigDecimal.ONE);
    }
}
