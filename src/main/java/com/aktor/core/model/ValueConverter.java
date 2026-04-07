package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.data.Relation;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.ModelException;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.lang.reflect.Array;
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
        return convertInternal(target, raw, key, field);
    }

    public Object convertComponent(
        final RowMappingContext context,
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
                try (RelationTraversalGuard.Scope ignored = RelationTraversalGuard.enterRead(context.itemType(), key, relationField))
                {
                    return convertInternal(target, raw, key, relationField);
                }
            }
            return convertInternal(target, raw, key, relationField);
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
        final String field
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
                    : relationProvider.single(key);
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
                final Data<?>[] relatedItems = relationProvider.many(key);
                final int count = relatedItems.length;
                final Object[] array = (Object[]) Array.newInstance(target.getComponentType(), count);
                System.arraycopy(relatedItems, 0, array, 0, count);
                object = array;
            }
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
            object = Enum.valueOf(enumClass.asSubclass(Enum.class), raw);
        }
        else
        {
            throw new ConversionException("Unsupported target type: " + target.getName());
        }

        return object;
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
