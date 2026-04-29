package com.aktor.core.data;

import java.util.Objects;

public record PersistenceEvent<Key, Target>(
    Key key,
    String subject,
    String operation,
    Target target,
    long timestamp
)
implements Event<Key, Target>
{
    public PersistenceEvent
    {
        key = Objects.requireNonNull(key);
        subject = Objects.requireNonNull(subject);
        operation = Objects.requireNonNull(operation);
        target = Objects.requireNonNull(target);
    }
}
