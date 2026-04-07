package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Model;
import com.aktor.core.exception.ModelException;

import java.util.Objects;
import java.util.Map;

public final class RelationProcessor<Key>
implements Model, TransactionParticipant
{
    private final RelationProviderResolver<Key> resolver;

    private final boolean syncEnabled;

    public RelationProcessor()
    {
        this(Map.of());
    }

    public RelationProcessor(final Map<String, ? extends RelationProvider<Key, ?, ?>> map)
    {
        this(new RelationProviderResolver<>(map));
    }

    public RelationProcessor(final RelationProviderResolver<Key> resolver)
    {
        this(resolver, true);
    }

    private RelationProcessor(final RelationProviderResolver<Key> resolver, final boolean syncEnabled)
    {
        super();
        this.resolver = Objects.requireNonNull(resolver);
        this.syncEnabled = syncEnabled;
    }

    public static <Key> RelationProcessor<Key> noOp()
    {
        return new RelationProcessor<>(new RelationProviderResolver<>(), false);
    }

    public void save(final Data<Key> item) throws ModelException
    {
        if (syncEnabled)
        {
            resolver.save(item);
        }
    }

    public void beforeDelete(final Data<Key> item) throws ModelException
    {
        if (syncEnabled)
        {
            resolver.beforeDelete(item);
        }
    }

    public void afterDelete(final Data<Key> item) throws ModelException
    {
        if (syncEnabled)
        {
            resolver.afterDelete(item);
        }
    }

    @Override
    public void beginTransaction() throws Exception
    {
        resolver.beginTransaction();
    }

    @Override
    public void commitTransaction() throws Exception
    {
        resolver.commitTransaction();
    }

    @Override
    public void rollbackTransaction() throws Exception
    {
        resolver.rollbackTransaction();
    }
}
