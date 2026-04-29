package com.aktor.core.service;

import com.aktor.core.EventRouteResolver;
import com.aktor.core.Observer;
import com.aktor.core.data.Event;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BroadcasterRegistry<RouteKey, EventId, Target, ObserverKey>
implements Broadcaster<RouteKey, EventId, Target, ObserverKey>
{
    private final EventRouteResolver<RouteKey, EventId, Target> routeResolver;
    private final ObserverRegistry<EventId, Target, ObserverKey> observerRegistry;
    private final ConcurrentHashMap<RouteKey, Set<ObserverKey>> routeObservers = new ConcurrentHashMap<>();

    public BroadcasterRegistry(
        final EventRouteResolver<RouteKey, EventId, Target> routeResolver,
        final ObserverRegistry<EventId, Target, ObserverKey> observerRegistry
    )
    {
        this.routeResolver = Objects.requireNonNull(routeResolver);
        this.observerRegistry = Objects.requireNonNull(observerRegistry);
    }

    @Override
    public void broadcast(final Event<EventId, Target> event)
    {
        final RouteKey routeKey = routeResolver.resolve(event);
        if (routeKey == null)
        {
            return;
        }
        final Set<ObserverKey> observerKeys = routeObservers.get(routeKey);
        if (observerKeys == null || observerKeys.isEmpty())
        {
            return;
        }
        for (final ObserverKey observerKey : observerKeys)
        {
            final Observer<EventId, Target, ObserverKey> observer = observerRegistry.get(observerKey);
            if (observer != null)
            {
                observer.handle(event);
            }
        }
    }

    @Override
    public void register(final RouteKey routeKey, final ObserverKey observerKey)
    {
        routeObservers.computeIfAbsent(Objects.requireNonNull(routeKey), ignored -> ConcurrentHashMap.newKeySet())
            .add(Objects.requireNonNull(observerKey));
    }

    @Override
    public boolean unregister(final RouteKey routeKey, final ObserverKey observerKey)
    {
        if (routeKey == null || observerKey == null)
        {
            return false;
        }
        final Set<ObserverKey> observerKeys = routeObservers.get(routeKey);
        if (observerKeys == null)
        {
            return false;
        }
        final boolean removed = observerKeys.remove(observerKey);
        if (observerKeys.isEmpty())
        {
            routeObservers.remove(routeKey, observerKeys);
        }
        return removed;
    }

    @Override
    public void clearRegistrations()
    {
        routeObservers.clear();
    }
}
