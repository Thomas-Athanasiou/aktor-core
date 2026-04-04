package com.aktor.core.service;

import com.aktor.core.data.Event;

public interface Broadcaster<RouteKey, EventId, Target, ObserverKey>
{
    void broadcast(Event<EventId, Target> event);

    void register(RouteKey routeKey, ObserverKey observerKey);

    boolean unregister(RouteKey routeKey, ObserverKey observerKey);

    void clearRegistrations();
}
