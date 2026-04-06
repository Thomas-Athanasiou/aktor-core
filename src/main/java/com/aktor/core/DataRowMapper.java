package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.RecordTypePlan;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.util.Objects;

public final class DataRowMapper<Item extends Data<Key>, Key>
implements Converter<Item, DataRow>
{
    private static final Value[] VALUES = new Value[0];

    private final FieldNormalizer fieldNameResolver;

    public DataRowMapper()
    {
        this(FieldNormalizer.DEFAULT);
    }

    public DataRowMapper(final FieldNormalizer fieldResolver)
    {
        this((FieldNormalizer) fieldResolver);
    }

    public DataRowMapper(final Class<? extends Data<?>> type)
    {
        this(FieldNormalizer.mapped(type));
    }

    private DataRowMapper(final FieldNormalizer fieldNameResolver)
    {
        this.fieldNameResolver = Objects.requireNonNull(fieldNameResolver);
    }

    @Override
    public DataRow convert(final Item item) throws ConversionException
    {
        final DataRow dataRow;
        try
        {
            final Class<?> type = item.getClass();
            final RecordTypePlan plan = RecordTypePlan.of(type);
            final int size = plan.size();
            final Value[] values = new Value[size];
            int valueCount = 0;

            for (int index = 0; index < size; index++)
            {
                final Object object = plan.readComponent(index, item);
                if (object != null && isScalarComponent(type, plan.componentType(index)))
                {
                    values[valueCount++] = new Value(
                        fieldNameResolver.resolve(plan.componentName(index)),
                        SimpleDataObjectConverter.objectToString(object)
                    );
                }
            }

            dataRow = DataRow.of(valueCount == size ? values : java.util.Arrays.copyOf(values, valueCount));
        }
        catch (final IllegalArgumentException exception)
        {
            throw new ConversionException(exception);
        }
        return dataRow;
    }

    private static boolean isScalarComponent(final Class<?> ownerType, final Class<?> componentType)
    {
        return SimpleDataObjectConverter.isPersistableType(componentType)
            || (RelationKeySpec.isRelationType(ownerType) && Object.class.equals(componentType));
    }
}
