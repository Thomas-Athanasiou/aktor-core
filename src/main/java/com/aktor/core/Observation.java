package com.aktor.core;

public interface Observation<Id, RouteKey, ObserverKey>
extends Data<Id>
{
    RouteKey routeKey();

    ObserverKey observerKey();

    boolean enabled();
}
