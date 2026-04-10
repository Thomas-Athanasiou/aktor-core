package com.aktor.core.model;

@FunctionalInterface
public interface ActionResolver<Route, Target>
{
    Route resolve(Target target);
}
