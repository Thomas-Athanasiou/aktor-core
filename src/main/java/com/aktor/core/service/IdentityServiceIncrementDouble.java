package com.aktor.core.service;

public final class IdentityServiceIncrementDouble
extends IdentityServiceIncrement<Double>
{
    public IdentityServiceIncrementDouble()
    {
        this(1.0d);
    }

    public IdentityServiceIncrementDouble(final double initialValue)
    {
        super(initialValue);
    }

    @Override
    protected Double increment(final Double current)
    {
        return current + 1.0d;
    }
}
