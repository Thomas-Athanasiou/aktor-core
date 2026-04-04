package com.aktor.core.service;

import com.aktor.core.ObserverDescriptor;
import com.aktor.core.data.Event;

public interface BroadcastTransport<EventId, Target, ObserverKey>
{
    boolean supports(ObserverDescriptor<?, ObserverKey> observerDescriptor);

    void send(ObserverDescriptor<?, ObserverKey> observerDescriptor, Event<EventId, Target> event);
}
