package com.aktor.core.data;

import com.aktor.core.Data;

import java.util.Objects;

public record ScopeItem<Key, Payload extends Data<Key>>(
    Key key,
    Scope scope,
    Payload payload
)
implements Data<Key>
{
    public ScopeItem
    {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(payload, "payload");
    }
}
