package com.aktor.core;

public interface Observer<EventId, Target, ObserverKey>
extends EventHandler<EventId, Target>
{
    ObserverKey key();
}
