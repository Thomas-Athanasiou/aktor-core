package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.RelationTraversalContext;
import com.aktor.core.model.RowMappingContext;

import java.util.Objects;

public interface ValueConverter<Key>
extends Converter<ValueConverter.Context<Key>, Object>
{
    @Override
    Object convert(Context<Key> context) throws ConversionException;

    Key toKey(String rawKey, String keyField, Class<?> itemType) throws ConversionException;

    record Context<Key>(
        RowMappingContext rowContext,
        RelationTraversalContext traversalContext,
        String componentName,
        String componentSnakeName,
        Class<?> target,
        String raw,
        Key key,
        String sourceField
    )
    {
        public Context
        {
            Objects.requireNonNull(rowContext);
            Objects.requireNonNull(traversalContext);
            Objects.requireNonNull(componentName);
            Objects.requireNonNull(componentSnakeName);
            Objects.requireNonNull(target);
            Objects.requireNonNull(key);
            Objects.requireNonNull(sourceField);
        }
    }
}
