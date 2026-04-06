package com.aktor.core.model;

import com.aktor.core.ConditionType;
import com.aktor.core.Data;
import com.aktor.core.FilterGroup;
import com.aktor.core.Model;
import com.aktor.core.Repository;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.SortOrder;
import com.aktor.core.data.Relation;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.ModelException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.service.Management;
import com.aktor.core.value.Filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public final class RelationProvider<MainKey, ForeignKey, ForeignData extends Data<ForeignKey>>
implements Model
{
    private static final SortOrder[] SORT_ORDERS = new SortOrder[0];

    private static final Data<?>[] DATA = new Data<?>[0];

    private final Class<ForeignData> foreignType;

    private final Management<ForeignData, ForeignKey> foreignManagement;

    private final Repository<Relation<MainKey, ForeignKey>, String> relationRepository;

    private final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory;
    private final String mainField;
    private final String foreignField;
    private final RelationSavePolicy savePolicy;
    private final RelationDeletePolicy deletePolicy;
    private final List<TransactionParticipant> transactionParticipants;

    public RelationProvider(final RelationBinding<MainKey, ForeignKey, ForeignData> binding)
    {
        this(
            binding.foreignType(),
            binding.foreignManagement(),
            binding.relationRepository(),
            binding.relationFactory(),
            binding.mainField(),
            binding.foreignField(),
            binding.savePolicy(),
            binding.deletePolicy()
        );
    }

    public RelationProvider(
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory,
        final String mainField,
        final String foreignField,
        final RelationSavePolicy savePolicy,
        final RelationDeletePolicy deletePolicy
    )
    {
        super();
        this.foreignType = Objects.requireNonNull(foreignType);
        this.foreignManagement = Objects.requireNonNull(foreignManagement);
        this.relationRepository = Objects.requireNonNull(relationRepository);
        this.relationFactory = Objects.requireNonNull(relationFactory);
        this.mainField = requireField(mainField);
        this.foreignField = requireField(foreignField);
        this.savePolicy = Objects.requireNonNull(savePolicy);
        this.deletePolicy = Objects.requireNonNull(deletePolicy);
        this.transactionParticipants = List.copyOf(
            TransactionParticipantUtil.collect(List.of(this.foreignManagement, this.relationRepository))
        );
    }

    public Data<?> single(final MainKey key) throws ModelException
    {
        final Data<?> item;
        final Data<?>[] items = many(key);
        if (items.length < 1)
        {
            item = null;
        }
        else if (items.length > 1)
        {
            throw new ModelException("One to one relation mismatch");
        }
        else
        {
            item = items[0];
        }
        return item;
    }

    public Data<?>[] many(final MainKey key) throws ModelException
    {
        try
        {
            final List<Relation<MainKey, ForeignKey>> list = getRelations(key).items();
            if (list.isEmpty())
            {
                return DATA;
            }
            final List<Data<?>> items = new ArrayList<>(list.size());
            for (final Relation<MainKey, ForeignKey> relation : list)
            {
                try
                {
                    try (RelationTraversalGuard.Scope ignored = RelationTraversalGuard.enterRead(foreignType, relation.foreignKey(), foreignField))
                    {
                        items.add(getForeignRepositoryItem(relation.foreignKey()));
                    }
                }
                catch (final GetException ignored)
                {
                    // Ignore orphan relations during reads so one stale foreign key
                    // does not fail the whole parent item mapping.
                    try
                    {
                        deleteRelation(relation);
                    }
                    catch (final ModelException cleanupException)
                    {
                        // Best-effort cleanup only.
                    }
                }
            }
            return items.toArray(DATA);
        }
        catch (final SearchException exception)
        {
            throw new ModelException(exception);
        }
    }

    void save(final MainKey mainKey, final Object value) throws ModelException
    {
        executeRelationMutation(
            () -> {
                final Collection<ForeignKey> foreignKeys = new HashSet<>();
                if (value instanceof final Data<?> data)
                {
                    foreignKeys.add(saveForeign(data));
                }
                else if (value instanceof final Data<?>[] array)
                {
                    for (final Data<?> data : array)
                    {
                        foreignKeys.add(saveForeign(data));
                    }
                }
                else if (value != null)
                {
                    throw new ModelException("Unsupported relation type for relation key: " + mainKey);
                }

                if (!usesInlineSingularRelationStorage())
                {
                    reconcileRelations(mainKey, loadExistingRelations(mainKey), foreignKeys);
                }
            }
        );
    }

    void beforeDelete(final MainKey mainKey) throws ModelException
    {
        if (deletePolicy.equals(RelationDeletePolicy.RESTRICT) && !loadExistingRelations(mainKey).isEmpty())
        {
            throw new ModelException("Cannot delete parent item with related items for field: " + foreignField);
        }
    }

    void afterDelete(final MainKey mainKey) throws ModelException
    {
        if (deletePolicy.equals(RelationDeletePolicy.CASCADE))
        {
            executeRelationMutation(
                () -> {
                    for (final Relation<MainKey, ForeignKey> existing : loadExistingRelations(mainKey))
                    {
                        deleteRelation(existing);
                    }
                }
            );
        }
    }

    SearchResult<Relation<MainKey, ForeignKey>> getRelations(final Object key) throws SearchException
    {
        if (key == null)
        {
            throw new SearchException("Relation key cannot be null");
        }
        return relationRepository.search(
            new SearchCriteria(
                new FilterGroup[]
                    {
                        new FilterGroup(
                            new Filter[]
                            {
                                new Filter(mainField, key.toString(), ConditionType.EQUALS)
                            }
                        )
                    },
                Integer.MAX_VALUE,
                1,
                SORT_ORDERS
            )
        );
    }

    private Data<?> getForeignRepositoryItem(final ForeignKey key) throws GetException
    {
        return foreignManagement.get(key);
    }

    private ForeignKey saveForeign(final Data<?> data) throws ModelException
    {
        final ForeignData item;
        try
        {
            item = foreignType.cast(data);
        }
        catch (final ClassCastException exception)
        {
            throw new ModelException(exception);
        }

        if (savePolicy.equals(RelationSavePolicy.REFERENCE))
        {
            return item.key();
        }

        if (savePolicy.equals(RelationSavePolicy.RESTRICT) && !foreignManagement.exists(item.key()))
        {
            throw new ModelException("Referenced item does not exist for relation field: " + foreignField);
        }

        try
        {
            try (RelationTraversalGuard.Scope ignored = RelationTraversalGuard.enterSave(
                foreignType,
                item.key(),
                foreignField
            ))
            {
                foreignManagement.save(item);
            }
        }
        catch (final SaveException exception)
        {
            throw new ModelException(exception);
        }
        return item.key();
    }

    private void saveRelation(final MainKey mainKey, final ForeignKey foreignKey) throws ModelException
    {
        try
        {
            relationRepository.save(relationFactory.apply(mainKey, foreignKey));
        }
        catch (final SaveException exception)
        {
            throw new ModelException(exception);
        }
    }

    private void deleteRelation(final Relation<MainKey, ForeignKey> relation) throws ModelException
    {
        try
        {
            relationRepository.delete(relation);
        }
        catch (final DeleteException exception)
        {
            throw new ModelException(exception);
        }
    }

    private void reconcileRelations(
        final MainKey mainKey,
        final Collection<Relation<MainKey, ForeignKey>> existingRelations,
        final Collection<ForeignKey> foreignKeys
    )
    throws ModelException
    {
        final Collection<ForeignKey> existingKeys = new HashSet<>(existingRelations.size());
        for (final Relation<MainKey, ForeignKey> existing : existingRelations)
        {
            final ForeignKey existingKey = existing.foreignKey();
            existingKeys.add(existingKey);
            if (!foreignKeys.contains(existingKey))
            {
                deleteRelation(existing);
            }
        }
        for (final ForeignKey foreignKey : foreignKeys)
        {
            if (!existingKeys.contains(foreignKey))
            {
                saveRelation(mainKey, foreignKey);
            }
        }
    }

    private List<Relation<MainKey, ForeignKey>> loadExistingRelations(final MainKey mainKey) throws ModelException
    {
        try
        {
            return getRelations(mainKey).items();
        }
        catch (final SearchException exception)
        {
            throw new ModelException(exception);
        }
    }

    private void executeRelationMutation(final RelationMutation mutation) throws ModelException
    {
        try
        {
            TransactionOrchestrator.execute(getTransactionParticipants(), mutation::run);
        }
        catch (final Exception exception)
        {
            if (exception instanceof final ModelException modelException)
            {
                throw modelException;
            }
            throw new ModelException(exception);
        }
    }

    List<TransactionParticipant> getTransactionParticipants()
    {
        return transactionParticipants;
    }

    private static String requireField(final String field)
    {
        final String value = Objects.requireNonNull(field);
        if (value.isBlank())
        {
            throw new IllegalArgumentException("field cannot be blank");
        }
        return value;
    }

    boolean usesInlineSingularRelationStorage()
    {
        return "key".equals(mainField)
            && !"foreign_key".equals(foreignField);
    }

    @FunctionalInterface
    private interface RelationMutation
    {
        void run() throws Exception;
    }
}
