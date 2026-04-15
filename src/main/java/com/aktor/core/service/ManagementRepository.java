package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.RelationProcessor;
import com.aktor.core.model.TransactionParticipant;
import com.aktor.core.model.TransactionParticipantUtil;
import com.aktor.core.model.TransactionOrchestrator;

import java.util.Arrays;
import java.util.List;

public class ManagementRepository<Item extends Data<Key>, Key>
extends ManagementRelational<Item, Key>
implements TransactionParticipant
{
    public ManagementRepository(
        final Repository<Item, Key> repository,
        final RelationProcessor<Key> relationProcessor
    )
    {
        this(
            repository,
            relationProcessor,
            new com.aktor.core.model.CollectionProcessor<>(
                new com.aktor.core.model.SearchCriteriaCondition(),
                new com.aktor.core.DataRowMapper<>()
            )
        );
    }

    public ManagementRepository(
        final Repository<Item, Key> repository,
        final RelationProcessor<Key> relationProcessor,
        final com.aktor.core.model.CollectionProcessor<Item, Key> processor
    )
    {
        super(repository, relationProcessor, processor);
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

    @Override
    protected SearchResult<Item> searchNative(final SearchCriteria searchCriteria) throws SearchException
    {
        return repository.search(searchCriteria);
    }

    @Override
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

    @Override
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
