package com.aktor.core;

import com.aktor.core.data.Event;

@FunctionalInterface
public interface EventRouteResolver<Route, Key, Target>
{
    Route resolve(Event<Key, Target> event);
}
