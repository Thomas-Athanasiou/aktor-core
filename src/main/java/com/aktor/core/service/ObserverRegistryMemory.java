package com.aktor.core.service;

import com.aktor.core.Observer;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ObserverRegistryMemory<EventId, Target, ObserverKey>
implements ObserverRegistry<EventId, Target, ObserverKey>
{
    private final ConcurrentMap<ObserverKey, Observer<EventId, Target, ObserverKey>> observers = new ConcurrentHashMap<>();

    @Override
    public void register(final Observer<EventId, Target, ObserverKey> observer)
    {
        final Observer<EventId, Target, ObserverKey> safeObserver = Objects.requireNonNull(observer, "observer");
        observers.put(Objects.requireNonNull(safeObserver.key(), "observer.key"), safeObserver);
    }

    @Override
    public Observer<EventId, Target, ObserverKey> get(final ObserverKey observerKey)
    {
        return observerKey == null ? null : observers.get(observerKey);
    }

    @Override
    public boolean unregister(final ObserverKey observerKey)
    {
        return observerKey != null && observers.remove(observerKey) != null;
    }

    @Override
    public void clear()
    {
        observers.clear();
    }
}
