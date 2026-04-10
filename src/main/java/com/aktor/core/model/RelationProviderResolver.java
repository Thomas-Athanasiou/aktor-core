package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Model;
import com.aktor.core.exception.ModelException;
import com.aktor.core.util.RecordAccessorFallbackUtil;
import com.aktor.core.util.RecordTypeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RelationProviderResolver<Key>
implements Model, TransactionParticipant
{
    private static final Map<Class<?>, AccessorMetadata[]> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final AccessorMetadata[] ACCESSOR_METADATA = new AccessorMetadata[0];

    private final Map<String, RelationProvider<Key, ?, ?>> map;
    private final List<TransactionParticipant> transactionParticipants;

    public RelationProviderResolver()
    {
        this(Map.of());
    }

    public RelationProviderResolver(final Map<String, ? extends RelationProvider<Key, ?, ?>> map)
    {
        super();
        this.map = Map.copyOf(new HashMap<>(Objects.requireNonNull(map)));
        final Collection<Object> providers = new ArrayList<>(this.map.size());
        for (final RelationProvider<Key, ?, ?> relationProvider : this.map.values())
        {
            providers.addAll(relationProvider.getTransactionParticipants());
        }
        this.transactionParticipants = List.copyOf(TransactionParticipantUtil.collect(providers));
    }

    public static <Key> Builder<Key> builder()
    {
        return new Builder<>();
    }

    public RelationProvider<Key, ?, ?> getRelationProvider(final String field) throws ModelException
    {
        final RelationProvider<Key, ?, ?> relationProvider = map.get(field);
        if (relationProvider == null)
        {
            throw new ModelException("No relation provider found for field: " + field);
        }
        return relationProvider;
    }

    void save(final Data<Key> item) throws ModelException
    {
        final RelationTraversalContext traversalContext = new RelationTraversalContext();
        if (!map.isEmpty())
        {
            final Class<?> itemType = item.getClass();
            if (!RecordTypeUtil.isRecordType(itemType))
            {
                throw new ModelException("Only record data types are supported: " + itemType.getName());
            }

            final AccessorMetadata[] metadataList = getCachedFields(itemType);
            for (final AccessorMetadata metadata : metadataList)
            {
                try
                {
                    final Object value = metadata.read(item);
                    if (!metadata.relationType() && !(value instanceof Data<?> || value instanceof Data<?>[]))
                    {
                        continue;
                    }

                    final RelationProvider<Key, ?, ?> relationProvider = getRelationProviderOrNull(metadata.name());
                    if (relationProvider == null)
                    {
                        throw new ModelException("No relation provider found for field: " + metadata.name());
                    }
                    else
                    {
                        try (RelationTraversalContext.Scope ignored = traversalContext.enterSave(
                            itemType,
                            item.key(),
                            metadata.name(),
                            relationProvider.cyclePolicy()
                        ))
                        {
                            if (ignored.linked() && relationProvider.cyclePolicy() == RelationCyclePolicy.LINK_EXISTING)
                            {
                                continue;
                            }
                            relationProvider.save(item.key(), value, traversalContext);
                        }
                    }
                }
                catch (final ReflectiveOperationException exception)
                {
                    throw new ModelException(exception);
                }
            }
        }
    }

    void afterDelete(final Data<Key> item) throws ModelException
    {
        if (!map.isEmpty())
        {
            final Class<?> itemType = item.getClass();
            if (!RecordTypeUtil.isRecordType(itemType))
            {
                throw new ModelException("Only record data types are supported: " + itemType.getName());
            }

            final AccessorMetadata[] metadataList = getCachedFields(itemType);
            for (final AccessorMetadata metadata : metadataList)
            {
                if (!metadata.relationType())
                {
                    continue;
                }

                final RelationProvider<Key, ?, ?> relationProvider = getRelationProviderOrNull(metadata.name());
                if (relationProvider == null)
                {
                    throw new ModelException("No relation provider found for field: " + metadata.name());
                }
                else
                {
                    relationProvider.afterDelete(item.key());
                }
            }
        }
    }

    void beforeDelete(final Data<Key> item) throws ModelException
    {
        if (!map.isEmpty())
        {
            final Class<?> itemType = item.getClass();
            if (!RecordTypeUtil.isRecordType(itemType))
            {
                throw new ModelException("Only record data types are supported: " + itemType.getName());
            }

            final AccessorMetadata[] metadataList = getCachedFields(itemType);
            for (final AccessorMetadata metadata : metadataList)
            {
                if (!metadata.relationType())
                {
                    continue;
                }

                final RelationProvider<Key, ?, ?> relationProvider = getRelationProviderOrNull(metadata.name());
                if (relationProvider == null)
                {
                    throw new ModelException("No relation provider found for field: " + metadata.name());
                }
                else
                {
                    relationProvider.beforeDelete(item.key());
                }
            }
        }
    }

    private RelationProvider<Key, ?, ?> getRelationProviderOrNull(final String field)
    {
        return map.get(field);
    }

    List<TransactionParticipant> getTransactionParticipants()
    {
        return transactionParticipants;
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

    private static AccessorMetadata[] getCachedFields(final Class<?> type)
    {
        return FIELD_CACHE.computeIfAbsent(type, RelationProviderResolver::buildFieldMetadata);
    }

    private static AccessorMetadata[] buildFieldMetadata(final Class<?> type)
    {
        if (!RecordTypeUtil.isRecordType(type))
        {
            throw new IllegalArgumentException("Only record data types are supported: " + type.getName());
        }

        final RecordTypeUtil.ComponentDescriptor[] components = RecordTypeUtil.getRecordComponents(type);
        final AccessorMetadata[] metadataArray;
        if (components.length > 0)
        {
            final List<AccessorMetadata> metadata = new ArrayList<>(components.length);
            for (final RecordTypeUtil.ComponentDescriptor component : components)
            {
                final Method accessor = component.accessor();
                final Class<?> componentType = component.type();
                metadata.add(
                    new AccessorMetadata(
                        FieldNormalizer.DEFAULT.resolve(component.name()),
                        isRelationType(componentType),
                        accessor::invoke
                    )
                );
            }
            metadataArray = metadata.toArray(ACCESSOR_METADATA);
        }
        else
        {
            metadataArray = buildFieldMetadataFromAccessorMethods(type);
        }
        return metadataArray;
    }

    private static AccessorMetadata[] buildFieldMetadataFromAccessorMethods(final Class<?> type)
    {
        final List<Method> methods = RecordAccessorFallbackUtil.resolveAccessors(type);
        final List<AccessorMetadata> metadata = new ArrayList<>(methods.size());
        for (final Method method : methods)
        {
            metadata.add(
                new AccessorMetadata(
                    FieldNormalizer.DEFAULT.resolve(method.getName()),
                    isRelationType(method.getReturnType()),
                    method::invoke
                )
            );
        }
        if (metadata.isEmpty())
        {
            throw new IllegalArgumentException("Record metadata not found for type: " + type.getName());
        }
        return metadata.toArray(ACCESSOR_METADATA);
    }

    private static boolean isRelationType(final Class<?> fieldType)
    {
        return Data.class.isAssignableFrom(fieldType) || Data[].class.isAssignableFrom(fieldType);
    }

    record AccessorMetadata(String name, boolean relationType, ValueReader reader)
    {
        Object read(final Object value) throws ReflectiveOperationException
        {
            return reader.read(value);
        }
    }

    @FunctionalInterface
    private interface ValueReader
    {
        Object read(Object value) throws ReflectiveOperationException;
    }

    public static final class Builder<Key>
    {
        private final Map<String, RelationProvider<Key, ?, ?>> relationProviderMap = new HashMap<>();

        public Builder<Key> add(final String field, final RelationProvider<Key, ?, ?> relationProvider)
        {
            final String name = requireField(field);
            putUnique(name, Objects.requireNonNull(relationProvider));
            return this;
        }

        public Builder<Key> add(
            final String field,
            final String mappedField,
            final RelationProvider<Key, ?, ?> relationProvider
        )
        {
            final String name = requireField(field);
            final String alias = requireField(mappedField);
            final RelationProvider<Key, ?, ?> safeRelationProvider = Objects.requireNonNull(relationProvider);
            putUnique(name, safeRelationProvider);
            if (!alias.equals(name))
            {
                putUnique(alias, safeRelationProvider);
            }
            return this;
        }

        public <ForeignKey, ForeignData extends Data<ForeignKey>> Builder<Key> add(
            final RelationBinding<Key, ForeignKey, ForeignData> relationBinding
        )
        {
            Objects.requireNonNull(relationBinding);
            return add(relationBinding.field(), new RelationProvider<>(relationBinding));
        }

        public <ForeignKey, ForeignData extends Data<ForeignKey>> Builder<Key> add(
            final String field,
            final RelationBinding<Key, ForeignKey, ForeignData> relationBinding
        )
        {
            return add(requireField(field), new RelationProvider<>(Objects.requireNonNull(relationBinding)));
        }

        public RelationProviderResolver<Key> build()
        {
            return new RelationProviderResolver<>(relationProviderMap);
        }

        private void putUnique(final String field, final RelationProvider<Key, ?, ?> relationProvider)
        {
            if (relationProviderMap.putIfAbsent(field, relationProvider) != null)
            {
                throw new IllegalArgumentException("Duplicate relation provider field: " + field);
            }
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

    }
}
