package com.aktor.core;

public interface ObserverDescriptor<Id, ObserverKey>
extends Data<Id>
{
    ObserverKey observerKey();

    String transport();

    String destination();

    boolean enabled();
}
