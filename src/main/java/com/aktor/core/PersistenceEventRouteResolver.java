package com.aktor.core;

import com.aktor.core.data.Event;
import com.aktor.core.data.PersistenceEvent;

import java.util.Objects;

public final class PersistenceEventRouteResolver<Key, Target>
implements EventRouteResolver<String, Key, Target>
{
    @Override
    public String resolve(final Event<Key, Target> event)
    {
        final PersistenceEvent<Key, Target> persistenceEvent = asPersistenceEvent(event);
        return persistenceEvent.subject() + "." + persistenceEvent.operation();
    }

    private static <Key, Target> PersistenceEvent<Key, Target> asPersistenceEvent(final Event<Key, Target> event)
    {
        return (PersistenceEvent<Key, Target>) Objects.requireNonNull(event, "event");
    }
}
