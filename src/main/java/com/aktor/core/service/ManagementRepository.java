package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.ModelException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.RelationProcessor;
import com.aktor.core.model.TransactionParticipant;
import com.aktor.core.model.TransactionParticipantUtil;
import com.aktor.core.model.TransactionOrchestrator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ManagementRepository<Item extends Data<Key>, Key>
implements Management<Item, Key>, TransactionParticipant
{
    private final Repository<Item, Key> repository;

    private final RelationProcessor<Key> relationProcessor;

    public ManagementRepository(
        final Repository<Item, Key> repository,
        final RelationProcessor<Key> relationProcessor
    )
    {
        super();
        this.repository = Objects.requireNonNull(repository);
        this.relationProcessor = Objects.requireNonNull(relationProcessor);
    }

    public ManagementRepository(final Repository<Item, Key> repository)
    {
        this(repository, new RelationProcessor<>());
    }

    public static <Item extends Data<Key>, Key> ManagementRepository<Item, Key> noRelations(
        final Repository<Item, Key> repository
    )
    {
        return new ManagementRepository<>(repository, RelationProcessor.noOp());
    }

    public Item get(final Key key) throws GetException
    {
        return repository.get(key);
    }

    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        return repository.search(searchCriteria);
    }

    public void delete(final Item item) throws DeleteException
    {
        try
        {
            TransactionOrchestrator.execute(
                getTransactionParticipants(),
                () -> {
                    relationProcessor.beforeDelete(item);
                    repository.delete(item);
                    relationProcessor.afterDelete(item);
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

    public void save(final Item item) throws SaveException
    {
        try
        {
            TransactionOrchestrator.execute(
                getTransactionParticipants(),
                () -> {
                    repository.save(item);
                    relationProcessor.save(item);
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

    public final boolean exists(final Key key)
    {
        boolean result;
        try
        {
            result = Objects.equals(get(key).key(), key);
        }
        catch (final GetException e)
        {
            result = false;
        }
        return result;
    }

    private List<TransactionParticipant> getTransactionParticipants()
    {
        return TransactionParticipantUtil.collect(Arrays.asList(repository, relationProcessor));
    }

    @Override
    public void beginTransaction() throws Exception
    {
        for (final TransactionParticipant participant : getTransactionParticipants())
        {
            participant.beginTransaction();
        }
    }

    @Override
    public void commitTransaction() throws Exception
    {
        final List<TransactionParticipant> participants = getTransactionParticipants();
        for (int index = participants.size() - 1; index >= 0; index--)
        {
            participants.get(index).commitTransaction();
        }
    }

    @Override
    public void rollbackTransaction() throws Exception
    {
        final List<TransactionParticipant> participants = getTransactionParticipants();
        for (int index = participants.size() - 1; index >= 0; index--)
        {
            participants.get(index).rollbackTransaction();
        }
    }
}
