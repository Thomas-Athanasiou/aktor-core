package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.model.TransactionParticipant;
import com.aktor.core.model.TransactionParticipantUtil;
import com.aktor.core.model.TransactionOrchestrator;

import java.util.List;
import java.util.Objects;

public abstract class RepositoryComposite<Item extends Data<Key>, Key>
implements Repository<Item, Key>
{
    private final List<Repository<Item, Key>> repositories;
    private volatile List<Repository<Item, Key>> transactionParticipantSource = null;
    private volatile List<TransactionParticipant> transactionParticipants = null;

    protected RepositoryComposite(final List<Repository<Item, Key>> repositoryList)
    {
        super();
        this.repositories = List.copyOf(Objects.requireNonNull(repositoryList));
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        try
        {
            executeTransactional(
                () -> {
                    for (final Repository<Item, Key> repository : getRepositories())
                    {
                        repository.save(item);
                    }
                }
            );
        }
        catch (final Exception exception)
        {
            if (exception instanceof final SaveException saveException)
            {
                throw saveException;
            }
            throw new SaveException(exception);
        }
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        try
        {
            executeTransactional(
                () -> {
                    for (final Repository<Item, Key> repository : getRepositories())
                    {
                        repository.delete(item);
                    }
                }
            );
        }
        catch (final Exception exception)
        {
            if (exception instanceof final DeleteException deleteException)
            {
                throw deleteException;
            }
            throw new DeleteException(exception);
        }
    }

    protected List<Repository<Item, Key>> getRepositories()
    {
        return repositories;
    }

    private List<TransactionParticipant> getTransactionParticipants()
    {
        final List<Repository<Item, Key>> currentRepositories = getRepositories();
        final List<TransactionParticipant> cachedParticipants = transactionParticipants;
        if (cachedParticipants != null && currentRepositories == transactionParticipantSource)
        {
            return cachedParticipants;
        }
        final List<TransactionParticipant> resolvedParticipants = TransactionParticipantUtil.collect(currentRepositories);
        transactionParticipantSource = currentRepositories;
        transactionParticipants = resolvedParticipants;
        return resolvedParticipants;
    }

    private void executeTransactional(final TransactionalAction action) throws Exception
    {
        TransactionOrchestrator.execute(getTransactionParticipants(), action::run);
    }

    @FunctionalInterface
    private interface TransactionalAction
    {
        void run() throws Exception;
    }
}
