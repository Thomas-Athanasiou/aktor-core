package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.data.Relation;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.ModelException;
import com.aktor.core.util.SimpleDataObjectConverter;
import com.aktor.core.util.CsvValuesUtil;

import java.lang.reflect.Array;
import java.util.Locale;
import java.util.Objects;

public final class ValueConverter<Key>
{
    private final RelationProviderResolver<Key> relationProviderResolver;

    private final Class<Key> keyType;

    public ValueConverter(final RelationProviderResolver<Key> relationProviderResolver, final Class<Key> keyType)
    {
        this.relationProviderResolver = Objects.requireNonNull(relationProviderResolver);
        this.keyType = Objects.requireNonNull(keyType);
    }

    public Object convert(final Class<?> target, final String raw, final Key key, final String field) throws ConversionException, ModelException
    {
        return convertInternal(target, raw, key, field, new RelationTraversalContext());
    }

    public Object convertComponent(
        final RowMappingContext context,
        final RelationTraversalContext traversalContext,
        final String componentName,
        final String componentSnakeName,
        final Class<?> target,
        final String raw,
        final Key key,
        final String sourceField
    ) throws ConversionException, ModelException
    {
        final String relationField = componentSnakeName == null || componentSnakeName.isBlank() ? sourceField : componentSnakeName;
        try
        {
            if (Object.class.equals(target) && context != null && Relation.class.isAssignableFrom(context.itemType()))
            {
                return raw;
            }
            if (context != null && (Data.class.isAssignableFrom(target) || Data[].class.isAssignableFrom(target)))
            {
                final RelationProvider<Key, ?, ?> relationProvider = relationProviderResolver.getRelationProvider(relationField);
                try (RelationTraversalContext.Scope ignored = traversalContext.enterRead(
                    context.itemType(),
                    key,
                    relationField,
                    relationProvider.cyclePolicy()
                ))
                {
                    if (ignored.linked() && relationProvider.cyclePolicy() == RelationCyclePolicy.LINK_EXISTING)
                    {
                        return Data[].class.isAssignableFrom(target) ? Array.newInstance(target.getComponentType(), 0) : null;
                    }
                    return convertInternal(target, raw, key, relationField, traversalContext);
                }
            }
            return convertInternal(target, raw, key, relationField, traversalContext);
        }
        catch (final ConversionException | RuntimeException exception)
        {
            throw new ConversionException(buildComponentMessage(context, componentName, sourceField, target, raw), exception);
        }
        catch (final ModelException exception)
        {
            throw new ModelException(buildComponentMessage(context, componentName, sourceField, target, raw), exception);
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

    private Object convertInternal(
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
                array[index] = convertEnum(componentType.asSubclass(Enum.class), values[index]);
            }
            object = array;
        }
        else if (raw == null || raw.isEmpty())
        {
            if (target.isPrimitive())
            {
                throw new ConversionException("Missing value for primitive field of: " + target.getName());
            }
            else
            {
                object = null;
            }
        }
        else if (SimpleDataObjectConverter.isScalar(target))
        {
            object = SimpleDataObjectConverter.convert(target, raw);
        }
        else if (Enum.class.isAssignableFrom(target))
        {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) target;
            try
            {
            object = convertEnum(enumClass.asSubclass(Enum.class), raw);
            }
            catch (final IllegalArgumentException exception)
            {
                throw new ConversionException(exception);
            }
        }
        else
        {
            throw new ConversionException("Unsupported target type: " + target.getName());
        }

        return object;
    }

    private static Enum<?> convertEnum(final Class<? extends Enum> enumType, final String raw)
    {
        final String safeRaw = raw == null ? "" : raw.trim();
        try
        {
            return Enum.valueOf(enumType, safeRaw);
        }
        catch (final IllegalArgumentException ignored)
        {
            final String normalized = safeRaw
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(java.util.Locale.US);
            return Enum.valueOf(enumType, normalized);
        }
    }

    public Key toKey(final String rawKey)
    throws ConversionException
    {
        return toKey(rawKey, null, null);
    }

    public Key toKey(final String rawKey, final String keyField, final Class<?> itemType)
    throws ConversionException
    {
        try
        {
            return keyType.cast(SimpleDataObjectConverter.convert(keyType, rawKey));
        }
        catch (final RuntimeException exception)
        {
            final String fieldPart = keyField == null || keyField.isBlank() ? "unknown" : keyField;
            final String itemPart = itemType == null ? "unknown" : itemType.getName();
            final String rawPart = String.valueOf(rawKey);
            throw new ConversionException(
                "Failed to convert key value '"
                    + rawPart
                    + "' to "
                    + keyType.getName()
                    + " (field="
                    + fieldPart
                    + ", itemType="
                    + itemPart
                    + ")",
                exception
            );
        }
    }
}
