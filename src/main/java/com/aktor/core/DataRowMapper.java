package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.ModelException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.RecordTypePlan;
import com.aktor.core.model.RelationCyclePolicy;
import com.aktor.core.model.RelationTraversalContext;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DataRowMapper<Item extends Data<Key>, Key>
implements Converter<Item, Row>
{
    private final FieldNormalizer fieldResolver;

    public DataRowMapper()
    {
        this(FieldNormalizer.DEFAULT);
    }

    public DataRowMapper(final FieldNormalizer fieldResolver)
    {
        this.fieldResolver = Objects.requireNonNull(fieldResolver);
    }

    @Override
    public Row convert(final Item item) throws ConversionException
    {
        final Row row;
        try
        {
            final List<Value> values = new ArrayList<>();
            appendValues(values, item, "", RecordTypePlan.of(item.getClass()), new RelationTraversalContext());
            row = Row.of(values);
        }
        catch (final IllegalArgumentException exception)
        {
            throw new ConversionException(exception);
        }
        return row;
    }

    private void appendValues(
        final List<Value> values,
        final Object item,
        final String fieldPrefix,
        final RecordTypePlan plan,
        final RelationTraversalContext traversalContext
    )
    throws ConversionException
    {
        final Class<?> type = item.getClass();
        final int size = plan.size();
        for (int index = 0; index < size; index++)
        {
            final Object object = plan.readComponent(index, item);
            if (object == null)
            {
                continue;
            }

            final String fieldName = fieldPrefix.isBlank()
                ? fieldResolver.resolve(plan.componentName(index))
                : fieldPrefix + "." + plan.componentSnakeName(index);
            appendValue(
                values,
                fieldName,
                object,
                type,
                plan.componentType(index),
                traversalContext
            );
        }
    }

    private void appendValue(
        final List<Value> values,
        final String fieldName,
        final Object object,
        final Class<?> ownerType,
        final Class<?> componentType,
        final RelationTraversalContext traversalContext
    ) throws ConversionException
    {
        if (object instanceof final Data<?> nestedItem)
        {
            try (RelationTraversalContext.Scope scope = traversalContext.enterRead(
                nestedItem.getClass(),
                nestedItem.key(),
                fieldName,
                RelationCyclePolicy.LINK_EXISTING
            ))
            {
                if (!scope.linked())
                {
                    appendValues(
                        values,
                        nestedItem,
                        fieldName,
                        RecordTypePlan.of(nestedItem.getClass()),
                        traversalContext
                    );
                }
            }
            catch (ModelException exception)
            {
                throw new ConversionException(exception);
            }
        }
        else if (componentType.isArray())
        {
            return;
        }
        else if (isScalarComponent(ownerType, componentType))
        {
            values.add(new Value(fieldName, SimpleDataObjectConverter.objectToString(object)));
        }
        else
        {
            throw new ConversionException(
                "Unsupported non-scalar component type " + componentType.getName()
                    + " in "
                    + ownerType.getName()
                    + " for field "
                    + fieldName
            );
        }
    }

    private static boolean isScalarComponent(final Class<?> ownerType, final Class<?> componentType)
    {
        return SimpleDataObjectConverter.isPersistableType(componentType)
            || (RelationKeySpec.isRelationType(ownerType) && Object.class.equals(componentType));
    }
}
