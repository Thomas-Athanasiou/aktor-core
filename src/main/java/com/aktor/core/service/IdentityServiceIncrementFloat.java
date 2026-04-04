package com.aktor.core.service;

public final class IdentityServiceIncrementFloat
extends IdentityServiceIncrement<Float>
{
    public IdentityServiceIncrementFloat()
    {
        this(1.0f);
    }

    public IdentityServiceIncrementFloat(final float initialValue)
    {
        super(initialValue);
    }

    @Override
    protected Float increment(final Float current)
    {
        return current + 1.0f;
    }
}
