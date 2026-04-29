package com.aktor.core;

import com.aktor.core.data.Event;
import com.aktor.core.data.PersistenceEvent;
import com.aktor.core.model.Logger;

import java.util.Objects;

public final class PersistenceEventObserver<Key, Target>
implements Observer<Key, Target, String>
{
    private final String key;
    private final Logger logger;

    public PersistenceEventObserver(final String key, final Logger logger)
    {
        this.key = Objects.requireNonNull(key, "key");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String key()
    {
        return key;
    }

    @Override
    public void handle(final Event<Key, Target> event)
    {
        final PersistenceEvent<Key, Target> persistenceEvent = asPersistenceEvent(event);
        logger.info(
            "Observed persistence event: key="
                + persistenceEvent.key()
                + ", subject="
                + persistenceEvent.subject()
                + ", operation="
                + persistenceEvent.operation()
                + ", timestamp="
                + persistenceEvent.timestamp()
        );
    }

    private static <Key, Target> PersistenceEvent<Key, Target> asPersistenceEvent(final Event<Key, Target> event)
    {
        return (PersistenceEvent<Key, Target>) Objects.requireNonNull(event, "event");
    }
}
