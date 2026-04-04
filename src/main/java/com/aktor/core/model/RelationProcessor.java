package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Model;
import com.aktor.core.exception.ModelException;
import java.util.List;
import java.util.Objects;
import java.util.Map;

public final class RelationProcessor<Key>
implements Model, TransactionParticipant
{
    private final RelationProviderResolver<Key> relationProvider;

    private final boolean relationSyncEnabled;

    public RelationProcessor()
    {
        this(Map.of());
    }

    public RelationProcessor(final Map<String, ? extends RelationProvider<Key, ?, ?>> relationProviderMap)
    {
        this(new RelationProviderResolver<>(relationProviderMap));
    }

    public RelationProcessor(final RelationProviderResolver<Key> relationProvider)
    {
        this(relationProvider, true);
    }

    private RelationProcessor(final RelationProviderResolver<Key> relationProvider, final boolean relationSyncEnabled)
    {
        super();
        this.relationProvider = Objects.requireNonNull(relationProvider);
        this.relationSyncEnabled = relationSyncEnabled;
    }

    public static <Key> RelationProcessor<Key> noOp()
    {
        return new RelationProcessor<>(new RelationProviderResolver<>(), false);
    }

    public void save(final Data<Key> item) throws ModelException
    {
        if (relationSyncEnabled)
        {
            relationProvider.save(item);
        }
    }

    public void beforeDelete(final Data<Key> item) throws ModelException
    {
        if (relationSyncEnabled)
        {
            relationProvider.beforeDelete(item);
        }
    }

    public void afterDelete(final Data<Key> item) throws ModelException
    {
        if (relationSyncEnabled)
        {
            relationProvider.afterDelete(item);
        }
    }

    @Override
    public void beginTransaction() throws Exception
    {
        relationProvider.beginTransaction();
    }

    @Override
    public void commitTransaction() throws Exception
    {
        relationProvider.commitTransaction();
    }

    @Override
    public void rollbackTransaction() throws Exception
    {
        relationProvider.rollbackTransaction();
    }
}
