package com.aktor.core.model;

@FunctionalInterface
public interface Factory<Request, Instance>
{
    Instance create(FactoryContext context, Request request);
}
