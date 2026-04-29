package com.aktor.core.service;

import com.aktor.core.data.Event;

public interface Broadcaster<RouteKey, EventKey, Target, ObserverKey>
{
    void broadcast(Event<EventKey, Target> event);

    void register(RouteKey routeKey, ObserverKey observerKey);

    boolean unregister(RouteKey routeKey, ObserverKey observerKey);

    void clearRegistrations();
}
