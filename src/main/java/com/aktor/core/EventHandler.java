package com.aktor.core;

import com.aktor.core.data.Event;

public interface EventHandler<Key, Target>
{
    void handle(Event<Key, Target> event);
}
