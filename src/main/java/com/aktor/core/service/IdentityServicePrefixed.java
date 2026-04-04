package com.aktor.core.service;

import java.util.Objects;

public final class IdentityServicePrefixed<Key>
implements IdentityService<String>
{
    private final String prefix;
    private final IdentityService<Key> delegate;

    public IdentityServicePrefixed(
        final String prefix,
        final IdentityService<Key> delegate
    )
    {
        this.prefix = Objects.requireNonNull(prefix);
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public String generate()
    {
        return prefix + delegate.generate();
    }
}
