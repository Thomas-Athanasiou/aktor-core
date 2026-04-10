package com.aktor.core.model;

public interface Router<Route, Target>
{
    void route(Route route, Target target);

    void route(Target target);
}
