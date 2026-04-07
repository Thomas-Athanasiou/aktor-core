package com.aktor.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class TransactionParticipantUtil
{
    private TransactionParticipantUtil()
    {
        super();
    }

    public static List<TransactionParticipant> collect(final Iterable<?> candidates)
    {
        final List<TransactionParticipant> participants = new ArrayList<>();
        final Set<TransactionParticipant> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final Object candidate : candidates)
        {
            addIfParticipant(participants, unique, candidate);
        }
        return participants;
    }

    public static void addIfParticipant(
        final Collection<TransactionParticipant> participants,
        final Collection<TransactionParticipant> unique,
        final Object candidate
    )
    {
        if (candidate instanceof final TransactionParticipant participant && unique.add(participant))
        {
            participants.add(participant);
        }
    }
}
