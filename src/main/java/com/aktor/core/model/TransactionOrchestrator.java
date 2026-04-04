package com.aktor.core.model;

import java.util.List;
import java.util.Objects;

public final class TransactionOrchestrator
{
    @FunctionalInterface
    public interface TransactionWork
    {
        void run() throws Exception;
    }

    private TransactionOrchestrator()
    {
        super();
    }

    public static void execute(
        final List<? extends TransactionParticipant> participants,
        final TransactionWork work
    ) throws Exception
    {
        Objects.requireNonNull(participants);
        Objects.requireNonNull(work);
        int rollbackStartIndex = participants.size() - 1;
        try
        {
            beginTransactions(participants);
            work.run();
            for (; rollbackStartIndex >= 0; rollbackStartIndex--)
            {
                participants.get(rollbackStartIndex).commitTransaction();
            }
        }
        catch (final Exception exception)
        {
            rollbackTransactions(participants, exception, rollbackStartIndex);
            throw exception;
        }
    }

    public static void rollbackTransactions(
        final List<? extends TransactionParticipant> participants,
        final Exception failure
    )
    {
        rollbackTransactions(participants, failure, participants.size() - 1);
    }

    private static void rollbackTransactions(
        final List<? extends TransactionParticipant> participants,
        final Exception failure,
        final int fromIndex
    )
    {
        for (int index = Math.min(fromIndex, participants.size() - 1); index >= 0; index--)
        {
            try
            {
                participants.get(index).rollbackTransaction();
            }
            catch (final Exception rollbackException)
            {
                failure.addSuppressed(rollbackException);
            }
        }
    }

    private static void beginTransactions(final Iterable<? extends TransactionParticipant> participants) throws Exception
    {
        for (final TransactionParticipant participant : participants)
        {
            participant.beginTransaction();
        }
    }
}
