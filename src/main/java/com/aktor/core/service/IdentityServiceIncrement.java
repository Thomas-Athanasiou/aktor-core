package com.aktor.core.service;

import java.util.Objects;

public abstract class IdentityServiceIncrement<Key>
implements IdentityService<Key>
{
    private Key value;

    protected IdentityServiceIncrement(final Key initialValue)
    {
        super();
        this.value = Objects.requireNonNull(initialValue);
    }

    @Override
    public synchronized Key generate()
    {
        final Key current = value;
        value = increment(current);
        return current;
    }

    protected abstract Key increment(Key current);
}

