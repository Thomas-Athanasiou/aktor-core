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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RelationProvider<MainKey, ForeignKey, ForeignData extends Data<ForeignKey>>
implements Model
{
    private static final String MAIN_FIELD = "main_key";

    private static final String FOREIGN_FIELD = "foreign_key";

    private static final SortOrder[] SORT_ORDERS = new SortOrder[0];

    private static final Data<?>[] DATA = new Data<?>[0];

    private final Class<ForeignData> foreignType;

    private final Management<ForeignData, ForeignKey> foreignManagement;

    private final Repository<Relation<MainKey, ForeignKey>, String> relationRepository;

    private final RelationFactory<MainKey, ForeignKey> relationFactory;
    private final String mainField;
    private final String foreignField;
    private final RelationCardinalityPolicy cardinalityPolicy;
    private final RelationStoragePolicy storagePolicy;
    private final RelationCyclePolicy cyclePolicy;
    private final RelationSavePolicy savePolicy;
    private final RelationDeletePolicy deletePolicy;
    private final List<TransactionParticipant> transactionParticipants;

    public RelationProvider(final RelationBinding<MainKey, ForeignKey, ForeignData> binding)
    {
        this(
            relationFieldResolver(binding.mainField(), binding.foreignField()),
            binding.foreignType(),
            binding.foreignManagement(),
            binding.relationRepository(),
            binding.relationFactory(),
            binding.cardinalityPolicy(),
            binding.storagePolicy(),
            binding.cyclePolicy(),
            binding.savePolicy(),
            binding.deletePolicy()
        );
    }

    public RelationProvider(
        final FieldNormalizer fieldResolver,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory,
        final RelationCardinalityPolicy cardinalityPolicy,
        final RelationStoragePolicy storagePolicy,
        final RelationCyclePolicy cyclePolicy,
        final RelationSavePolicy savePolicy,
        final RelationDeletePolicy deletePolicy
    )
    {
        super();
        this.foreignType = Objects.requireNonNull(foreignType);
        this.foreignManagement = Objects.requireNonNull(foreignManagement);
        this.relationRepository = Objects.requireNonNull(relationRepository);
        this.relationFactory = Objects.requireNonNull(relationFactory);

        Objects.requireNonNull(fieldResolver);
        this.mainField = requireField(fieldResolver.resolve(MAIN_FIELD));
        this.foreignField = requireField(fieldResolver.resolve(FOREIGN_FIELD));

        this.cardinalityPolicy = Objects.requireNonNull(cardinalityPolicy);
        this.storagePolicy = Objects.requireNonNull(storagePolicy);
        this.cyclePolicy = Objects.requireNonNull(cyclePolicy);
        this.savePolicy = Objects.requireNonNull(savePolicy);
        this.deletePolicy = Objects.requireNonNull(deletePolicy);
        this.transactionParticipants = List.copyOf(
            TransactionParticipantUtil.collect(List.of(this.foreignManagement, this.relationRepository))
        );
    }

    public RelationProvider(
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory,
        final RelationSavePolicy savePolicy,
        final RelationDeletePolicy deletePolicy
    )
    {
        this(
            relationFieldResolver(MAIN_FIELD, FOREIGN_FIELD),
            foreignType,
            foreignManagement,
            relationRepository,
            relationFactory,
            RelationCardinalityPolicy.ONE_TO_ONE,
            RelationStoragePolicy.SEPARATE,
            RelationCyclePolicy.REJECT,
            savePolicy,
            deletePolicy
        );
    }

    public Data<?> single(final MainKey key) throws ModelException
    {
        return single(key, new RelationTraversalContext());
    }

    Data<?> single(final MainKey key, final RelationTraversalContext traversalContext) throws ModelException
    {
        final Data<?>[] items = many(key, traversalContext);
        return items.length > 0 ? items[0] : null;
    }

    public Data<?>[] many(final MainKey key) throws ModelException
    {
        return many(key, new RelationTraversalContext());
    }

    Data<?>[] many(final MainKey key, final RelationTraversalContext traversalContext) throws ModelException
    {
        try
        {
            final List<Relation<MainKey, ForeignKey>> list = getRelations(key).items();
            if (list.isEmpty())
            {
                return DATA;
            }
            if (cardinalityPolicy == RelationCardinalityPolicy.ONE_TO_ONE && list.size() > 1)
            {
                throw new ModelException("One to one relation mismatch");
            }
            final List<Data<?>> items = new ArrayList<>(list.size());
            for (final Relation<MainKey, ForeignKey> relation : list)
            {
                try
                {
                    try (RelationTraversalContext.Scope ignored = traversalContext.enterRead(
                        foreignType,
                        relation.foreignKey(),
                        foreignField,
                        cyclePolicy
                    ))
                    {
                        if (ignored.linked() && cyclePolicy == RelationCyclePolicy.LINK_EXISTING)
                        {
                            continue;
                        }
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
        save(mainKey, value, new RelationTraversalContext());
    }

    void save(final MainKey mainKey, final Object value, final RelationTraversalContext traversalContext) throws ModelException
    {
        executeRelationMutation(
            () -> {
                final Collection<ForeignKey> foreignKeys = new HashSet<>();
                if (value instanceof final Data<?> data)
                {
                    foreignKeys.add(saveForeign(data, traversalContext));
                }
                else if (value instanceof final Data<?>[] array)
                {
                    for (final Data<?> data : array)
                    {
                        foreignKeys.add(saveForeign(data, traversalContext));
                    }
                }
                else if (value != null)
                {
                    throw new ModelException("Unsupported relation type for relation key: " + mainKey);
                }

                if (cardinalityPolicy == RelationCardinalityPolicy.ONE_TO_ONE && foreignKeys.size() > 1)
                {
                    throw new ModelException("One to one relation mismatch");
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

    private ForeignKey saveForeign(final Data<?> data, final RelationTraversalContext traversalContext) throws ModelException
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
            try (RelationTraversalContext.Scope ignored = traversalContext.enterSave(
                foreignType,
                item.key(),
                foreignField,
                cyclePolicy
            ))
            {
                if (ignored.linked() && cyclePolicy == RelationCyclePolicy.LINK_EXISTING)
                {
                    return item.key();
                }
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
            relationRepository.save(relationFactory.create(mainKey, foreignKey));
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
        final Collection<String> existingKeys = new HashSet<>(existingRelations.size());
        final Collection<String> desiredKeys = new HashSet<>(foreignKeys.size());
        for (final ForeignKey foreignKey : foreignKeys)
        {
            desiredKeys.add(fingerprint(foreignKey));
        }
        for (final Relation<MainKey, ForeignKey> existing : existingRelations)
        {
            final String existingKey = fingerprint(existing.foreignKey());
            existingKeys.add(existingKey);
            if (!desiredKeys.contains(existingKey))
            {
                deleteRelation(existing);
            }
        }
        for (final ForeignKey foreignKey : foreignKeys)
        {
            if (!existingKeys.contains(fingerprint(foreignKey)))
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

    private static String fingerprint(final Object value)
    {
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static FieldResolver relationFieldResolver(final String mainField, final String foreignField)
    {
        return FieldResolver.mapped(
            (Class<? extends Data<?>>) (Class<?>) Relation.class,
            Map.of(MAIN_FIELD, requireField(mainField), FOREIGN_FIELD, requireField(foreignField))
        );
    }

    boolean usesInlineSingularRelationStorage()
    {
        return storagePolicy == RelationStoragePolicy.INLINE;
    }

    RelationCyclePolicy cyclePolicy()
    {
        return cyclePolicy;
    }

    @FunctionalInterface
    private interface RelationMutation
    {
        void run() throws Exception;
    }
}
