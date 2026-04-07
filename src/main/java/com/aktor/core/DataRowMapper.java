package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.FieldResolver;
import com.aktor.core.model.RecordTypePlan;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.util.Objects;

public final class DataRowMapper<Item extends Data<Key>, Key>
implements Converter<Item, DataRow>
{
    private final FieldNormalizer fieldResolver;

    public DataRowMapper()
    {
        this(FieldNormalizer.DEFAULT);
    }

    public DataRowMapper(final Class<? extends Data<?>> type)
    {
        this(FieldResolver.mapped(type));
    }

    public DataRowMapper(final FieldNormalizer fieldResolver)
    {
        this.fieldResolver = Objects.requireNonNull(fieldResolver);
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
                        fieldResolver.resolve(plan.componentName(index)),
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
