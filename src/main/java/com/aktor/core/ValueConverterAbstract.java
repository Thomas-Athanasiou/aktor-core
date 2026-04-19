package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.ModelException;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.util.Locale;
import java.util.Objects;

public abstract class ValueConverterAbstract<Key>
implements ValueConverter<Key>
{
    private final Class<Key> keyType;

    protected ValueConverterAbstract(final Class<Key> keyType)
    {
        this.keyType = Objects.requireNonNull(keyType);
    }

    @Override
    public final Object convert(final Context<Key> context) throws ConversionException
    {
        try
        {
            return convertValue(Objects.requireNonNull(context, "context"));
        }
        catch (final ModelException exception)
        {
            throw new ConversionException(exception);
        }
    }

    protected abstract Object convertValue(Context<Key> context) throws ConversionException, ModelException;

    protected Object convertScalar(final Class<?> target, final String raw) throws ConversionException
    {
        if (target == String.class)
        {
            return raw;
        }
        if (raw == null)
        {
            if (target.isPrimitive())
            {
                throw new ConversionException("Missing value for primitive field of: " + target.getName());
            }
            return null;
        }
        if (raw.isBlank() && !target.isPrimitive())
        {
            return null;
        }
        if (SimpleDataObjectConverter.isScalar(target))
        {
            return SimpleDataObjectConverter.convert(target, raw);
        }
        if (Enum.class.isAssignableFrom(target))
        {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) target;
            return convertEnum(enumClass, raw);
        }
        throw new ConversionException("Unsupported target type: " + target.getName());
    }

    protected Key convertKey(final String rawKey) throws ConversionException
    {
        try
        {
            return keyType.cast(SimpleDataObjectConverter.convert(keyType, rawKey));
        }
        catch (final RuntimeException exception)
        {
            throw new ConversionException(exception);
        }
    }

    protected static boolean isDataType(final Class<?> target)
    {
        return Data.class.isAssignableFrom(target);
    }

    protected static String normalizeEnum(final String value)
    {
        return value.trim()
            .replace(' ', '_')
            .replace('-', '_')
            .toUpperCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    protected static Enum<?> convertEnum(final Class<? extends Enum<?>> enumClass, final String raw)
    {
        final String safeRaw = raw == null ? "" : raw.trim();
        try
        {
            return Enum.valueOf((Class) enumClass.asSubclass(Enum.class), safeRaw);
        }
        catch (final IllegalArgumentException ignored)
        {
            return Enum.valueOf((Class) enumClass.asSubclass(Enum.class), normalizeEnum(safeRaw));
        }
    }

    @Override
    public Key toKey(final String rawKey, final String keyField, final Class<?> itemType)
    throws ConversionException
    {
        try
        {
            return convertKey(rawKey);
        }
        catch (final ConversionException exception)
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
