package com.aktor.core.model;

import com.aktor.core.Action;

@FunctionalInterface
public interface ActionFactory<Route, Target>
{
    Action<? super Target> create(Route route);
}
