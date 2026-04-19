package com.aktor.core;

import com.aktor.core.data.Relation;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.ModelException;
import com.aktor.core.model.RelationCyclePolicy;
import com.aktor.core.model.RelationProvider;
import com.aktor.core.model.RelationProviderResolver;
import com.aktor.core.model.RelationTraversalContext;
import com.aktor.core.model.RowMappingContext;
import com.aktor.core.util.CsvValuesUtil;

import java.lang.reflect.Array;
import java.util.Objects;

public final class ValueConverterRelational<Key>
extends ValueConverterAbstract<Key>
{
    private final RelationProviderResolver<Key> relationProviderResolver;

    public ValueConverterRelational(
        final RelationProviderResolver<Key> relationProviderResolver,
        final Class<Key> keyType
    )
    {
        super(keyType);
        this.relationProviderResolver = Objects.requireNonNull(relationProviderResolver);
    }

    @Override
    protected Object convertValue(final Context<Key> context) throws ConversionException, ModelException
    {
        final RowMappingContext rowContext = context.rowContext();
        final RelationTraversalContext traversalContext = context.traversalContext();
        final String componentName = context.componentName();
        final String componentSnakeName = context.componentSnakeName();
        final Class<?> target = context.target();
        final String raw = context.raw();
        final Key key = context.key();
        final String sourceField = context.sourceField();
        final String relationField = componentSnakeName == null || componentSnakeName.isBlank() ? sourceField : componentSnakeName;
        try
        {
            if (Object.class.equals(target) && rowContext != null && Relation.class.isAssignableFrom(rowContext.itemType()))
            {
                return raw;
            }
            if (rowContext != null && (Data.class.isAssignableFrom(target) || Data[].class.isAssignableFrom(target)))
            {
                final RelationProvider<Key, ?, ?> relationProvider = relationProviderResolver.getRelationProvider(relationField);
                try (RelationTraversalContext.Scope ignored = traversalContext.enterRead(
                    rowContext.itemType(),
                    key,
                    relationField,
                    relationProvider.cyclePolicy()
                ))
                {
                    if (ignored.linked() && relationProvider.cyclePolicy() == RelationCyclePolicy.LINK_EXISTING)
                    {
                        return Data[].class.isAssignableFrom(target) ? Array.newInstance(target.getComponentType(), 0) : null;
                    }
                    return convertRelationInternal(target, raw, key, relationField, traversalContext);
                }
            }
            return convertRelationInternal(target, raw, key, relationField, traversalContext);
        }
        catch (final ConversionException | RuntimeException exception)
        {
            throw new ConversionException(buildComponentMessage(rowContext, componentName, sourceField, target, raw), exception);
        }
        catch (final ModelException exception)
        {
            throw new ModelException(buildComponentMessage(rowContext, componentName, sourceField, target, raw), exception);
        }
    }

    private static String buildComponentMessage(
        final RowMappingContext context,
        final String componentName,
        final String sourceField,
        final Class<?> target,
        final String raw
    )
    {
        final String itemType = context == null || context.itemType() == null ? "unknown" : context.itemType().getName();
        final String component = componentName == null || componentName.isBlank() ? "unknown" : componentName;
        final String field = sourceField == null || sourceField.isBlank() ? "unknown" : sourceField;
        return "Failed to map component '"
            + component
            + "' (field="
            + field
            + ", target="
            + target.getName()
            + ", raw="
            + raw
            + ", itemType="
            + itemType
            + ")";
    }

    private Object convertRelationInternal(
        final Class<?> target,
        final String raw,
        final Key key,
        final String field,
        final RelationTraversalContext traversalContext
    ) throws ConversionException, ModelException
    {
        final Object object;

        if (Data.class.isAssignableFrom(target))
        {
            if (field == null || field.isBlank())
            {
                object = null;
            }
            else
            {
                final RelationProvider<Key, ?, ?> relationProvider = relationProviderResolver.getRelationProvider(field);
                object = relationProvider.usesInlineSingularRelationStorage() && (raw == null || raw.isBlank())
                    ? null
                    : relationProvider.single(key, traversalContext);
            }
        }
        else if (Data[].class.isAssignableFrom(target))
        {
            if (field == null || field.isBlank())
            {
                object = null;
            }
            else
            {
                final RelationProvider<Key, ?, ?> relationProvider = relationProviderResolver.getRelationProvider(field);
                final Data<?>[] relatedItems = relationProvider.many(key, traversalContext);
                final int count = relatedItems.length;
                final Object[] array = (Object[]) Array.newInstance(target.getComponentType(), count);
                System.arraycopy(relatedItems, 0, array, 0, count);
                object = array;
            }
        }
        else if (target.isArray() && Enum.class.isAssignableFrom(target.getComponentType()))
        {
            final Class<?> componentType = target.getComponentType();
            final String[] values = CsvValuesUtil.split(raw);
            final Object[] array = (Object[]) Array.newInstance(componentType, values.length);
            for (int index = 0; index < values.length; index++)
            {
                @SuppressWarnings("unchecked")
                final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) componentType;
                array[index] = convertEnum(enumType, values[index]);
            }
            object = array;
        }
        else
        {
            object = convertScalar(target, raw);
        }

        return object;
    }
}
