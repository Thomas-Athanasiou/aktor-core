package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.ModelException;
import com.aktor.core.model.FieldResolver;
import com.aktor.core.model.RecordTypePlan;
import com.aktor.core.model.RelationProviderResolver;
import com.aktor.core.model.RelationTraversalContext;
import com.aktor.core.model.RowMappingContext;
import com.aktor.core.model.ValueConverter;
import com.aktor.core.util.RecordTypeUtil;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

public class Mapper<Item extends Data<Key>, Key>
implements Converter<Map<String, String>, Item>
{
    private static final String LOGICAL_KEY_FIELD_NAME = "key";

    private final FieldResolver resolver;

    private final ValueConverter<Key> converter;

    private final RecordTypePlan plan;

    private final Class<? extends Data<Key>> itemType;
    private final String[] componentNames;
    private final String[] componentSnakeNames;
    private final Class<?>[] componentTypes;
    private final int keyComponentIndex;
    private final String keyComponentName;
    private final String keyComponentSnakeName;

    public Mapper(
        final FieldResolver resolver,
        final RelationProviderResolver<Key> relationProviderResolver,
        final Class<? extends Data<Key>> itemType
    )
    {
        this(Objects.requireNonNull(resolver), new ValueConverter<>(relationProviderResolver, resolveKeyType(itemType)), itemType);
    }

    public Mapper(final FieldResolver resolver, final ValueConverter<Key> converter, final Class<? extends Data<Key>> type)
    {
        super();
        this.resolver = Objects.requireNonNull(resolver);
        this.converter = Objects.requireNonNull(converter);
        final Class<? extends Data<Key>> safeItemType = Objects.requireNonNull(type);
        this.itemType = safeItemType;
        if (!RecordTypeUtil.isRecordType(safeItemType))
        {
            throw new IllegalArgumentException("Mapper supports only record item types: " + safeItemType.getName());
        }

        try
        {
            this.plan = RecordTypePlan.of(safeItemType);
            final int size = plan.size();
            this.componentNames = new String[size];
            this.componentSnakeNames = new String[size];
            this.componentTypes = new Class<?>[size];
            for (int index = 0; index < size; index++)
            {
                componentNames[index] = plan.componentName(index);
                componentSnakeNames[index] = plan.componentSnakeName(index);
                componentTypes[index] = plan.componentType(index);
            }
            this.keyComponentIndex = plan.keyComponentIndex();
            this.keyComponentName = keyComponentIndex < 0 ? null : componentNames[keyComponentIndex];
            this.keyComponentSnakeName = keyComponentIndex < 0 ? null : componentSnakeNames[keyComponentIndex];
        }
        catch (final ConversionException exception)
        {
            throw new IllegalArgumentException("Unable to build mapper plan for: " + safeItemType.getName(), exception);
        }
    }

    public Mapper(final FieldResolver resolver, final Class<? extends Data<Key>> itemType)
    {
        this(resolver, new RelationProviderResolver<>(), itemType);
    }

    public Mapper(final RelationProviderResolver<Key> relationProviderResolver, final Class<? extends Data<Key>> itemType)
    {
        this(FieldResolver.mapped(itemType), relationProviderResolver, itemType);
    }

    public Mapper(final Class<? extends Data<Key>> itemType)
    {
        this(FieldResolver.mapped(itemType), itemType);
    }

    @Override
    public final Item convert(final Map<String, String> map) throws ConversionException
    {
        final Item item;
        try
        {
            final RowMappingContext context = new RowMappingContext(
                map,
                itemType,
                resolver.resolve(LOGICAL_KEY_FIELD_NAME)
            );
            final RelationTraversalContext traversalContext = new RelationTraversalContext();
            final int size = componentNames.length;
            final Object[] arguments = new Object[size];
            final String rawKey = resolver.resolveRawKey(
                context,
                keyComponentIndex,
                keyComponentName,
                keyComponentSnakeName
            );
            final Key key = converter.toKey(rawKey, keyComponentName, itemType);
            for (int index = 0; index < size; index++)
            {
                final String componentName = componentNames[index];
                final String componentSnakeName = componentSnakeNames[index];
                final FieldResolver.ResolvedValue resolved = resolver.resolveValue(
                    context,
                    index,
                    componentName,
                    componentSnakeName
                );
                arguments[index] = converter.convertComponent(
                    context,
                    traversalContext,
                    componentName,
                    componentSnakeName,
                    componentTypes[index],
                    resolved.rawValue(),
                    key,
                    resolved.field()
                );
            }

            @SuppressWarnings("unchecked")
            final Item created = (Item) plan.instantiate(arguments);
            item = created;
        }
        catch (final RuntimeException | ReflectiveOperationException | ModelException exception)
        {
            throw new ConversionException(exception);
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private static <Key> Class<Key> resolveKeyType(final Class<? extends Data<Key>> itemType)
    {
        try
        {
            final Class<?> methodType = itemType.getMethod(LOGICAL_KEY_FIELD_NAME).getReturnType();
            if (!Void.TYPE.equals(methodType))
            {
                return (Class<Key>) methodType;
            }

            final RecordTypePlan plan = RecordTypePlan.of(itemType);
            final int keyIndex = plan.keyComponentIndex();
            if (keyIndex < 0)
            {
                throw new IllegalArgumentException("No logical key component named 'key' on: " + itemType.getName());
            }
            return (Class<Key>) plan.componentType(keyIndex);
        }
        catch (final NoSuchMethodException | ConversionException exception)
        {
            throw new IllegalArgumentException("Unable to resolve key type for: " + itemType.getName(), exception);
        }
    }
}
