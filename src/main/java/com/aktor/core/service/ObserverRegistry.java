package com.aktor.core.service;

import com.aktor.core.Observer;

public interface ObserverRegistry<EventId, Target, ObserverKey>
{
    void register(Observer<EventId, Target, ObserverKey> observer);

    Observer<EventId, Target, ObserverKey> get(ObserverKey observerKey);

    boolean unregister(ObserverKey observerKey);

    void clear();
}
