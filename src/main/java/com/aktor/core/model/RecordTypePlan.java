package com.aktor.core.model;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.util.RecordTypeUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordTypePlan
{
    private static final Map<Class<?>, RecordTypePlan> CACHE = new ConcurrentHashMap<>();

    private final Constructor<?> constructor;

    private final String[] componentNames;

    private final String[] componentSnakeNames;

    private final Class<?>[] componentTypes;

    private final Method[] accessors;

    private final int keyComponentIndex;

    private RecordTypePlan(
        final Constructor<?> constructor,
        final String[] componentNames,
        final String[] componentSnakeNames,
        final Class<?>[] componentTypes,
        final Method[] accessors,
        final int keyComponentIndex
    )
    {
        this.constructor = constructor;
        this.componentNames = componentNames;
        this.componentSnakeNames = componentSnakeNames;
        this.componentTypes = componentTypes;
        this.accessors = accessors;
        this.keyComponentIndex = keyComponentIndex;
    }

    public static RecordTypePlan of(final Class<?> type) throws ConversionException
    {
        final RecordTypePlan plan;
        final RecordTypePlan cached = CACHE.get(type);
        if (cached == null)
        {
            final RecordTypePlan created;
            try
            {
                created = build(type);
            }
            catch (final ReflectiveOperationException exception)
            {
                throw new ConversionException(exception);
            }

            final RecordTypePlan existing = CACHE.putIfAbsent(type, created);
            plan = existing == null ? created : existing;
        }
        else
        {
            plan = cached;
        }
        return plan;
    }

    public int size()
    {
        return componentNames.length;
    }

    public String componentName(final int index)
    {
        return componentNames[index];
    }

    public String componentSnakeName(final int index)
    {
        return componentSnakeNames[index];
    }

    public Class<?> componentType(final int index)
    {
        return componentTypes[index];
    }

    public Object readComponent(final int index, final Object item) throws ConversionException
    {
        try
        {
            return accessors[index].invoke(item);
        }
        catch (final RuntimeException | Error exception)
        {
            throw exception;
        }
        catch (final Throwable throwable)
        {
            throw new ConversionException(
                throwable instanceof final Exception exception
                    ? exception
                    : new Exception(throwable)
            );
        }
    }

    public int keyComponentIndex()
    {
        return keyComponentIndex;
    }

    public Object instantiate(final Object[] arguments) throws ReflectiveOperationException
    {
        return constructor.newInstance(arguments);
    }

    private static RecordTypePlan build(final Class<?> type)
    throws ReflectiveOperationException, ConversionException
    {
        if (!RecordTypeUtil.isRecordType(type))
        {
            throw new ConversionException("Only record data types are supported: " + type.getName());
        }

        final RecordTypeUtil.ComponentDescriptor[] components = RecordTypeUtil.getRecordComponents(type);
        if (components.length > 0)
        {
            final Class<?>[] constructorTypes = Arrays.stream(components)
                .map(RecordTypeUtil.ComponentDescriptor::type)
                .toArray(Class<?>[]::new);
            final Constructor<?> constructor = type.getDeclaredConstructor(constructorTypes);
            return buildPlanFromComponents(constructor, components);
        }
        return buildPlanFromConstructorFallback(type);
    }

    private static RecordTypePlan buildPlanFromComponents(
        final Constructor<?> constructor,
        final RecordTypeUtil.ComponentDescriptor[] components
    ) throws IllegalAccessException
    {
        final int size = components.length;
        final String[] componentNames = new String[size];
        final String[] componentSnakeNames = new String[size];
        final Class<?>[] componentTypes = new Class<?>[size];
        final Method[] accessorMethods = new Method[size];
        int keyIndex = -1;
        for (int index = 0; index < size; index++)
        {
            final String name = components[index].name();
            componentNames[index] = name;
            componentSnakeNames[index] = FieldNormalizer.DEFAULT.resolve(name);
            componentTypes[index] = components[index].type();
            accessorMethods[index] = components[index].accessor();
            if (name.equals("key"))
            {
                keyIndex = index;
            }
        }
        return new RecordTypePlan(
            constructor,
            componentNames,
            componentSnakeNames,
            componentTypes,
            accessorMethods,
            keyIndex
        );
    }

    private static RecordTypePlan buildPlanFromConstructorFallback(final Class<?> type)
    throws ConversionException, IllegalAccessException
    {
        final Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 0)
        {
            throw new ConversionException("No constructor found for: " + type.getName());
        }

        final Constructor<?> constructor = Arrays.stream(constructors)
            .max(Comparator.comparingInt(value -> value.getParameterTypes().length))
            .orElseThrow(() -> new ConversionException("No constructor found for: " + type.getName()));

        final int size = constructor.getParameterTypes().length;
        final String[] componentNames = new String[size];
        final String[] componentSnakeNames = new String[size];
        final Class<?>[] componentTypes = constructor.getParameterTypes();
        final String[] fallbackComponentNames = RecordTypeUtil.getFallbackComponentNames(type);
        if (fallbackComponentNames.length != size)
        {
            throw new ConversionException("Record component metadata not found for type: " + type.getName());
        }
        final Method[] accessors = resolveAccessors(type, fallbackComponentNames);
        int keyIndex = -1;

        for (int index = 0; index < size; index++)
        {
            final String name = fallbackComponentNames[index];
            componentNames[index] = name;
            componentSnakeNames[index] = FieldNormalizer.DEFAULT.resolve(name);
            if ("key".equals(name))
            {
                keyIndex = index;
            }
        }

        return new RecordTypePlan(
            constructor,
            componentNames,
            componentSnakeNames,
            componentTypes,
            accessors,
            keyIndex
        );
    }

    private static Method[] resolveAccessors(final Class<?> type, final String[] componentNames) throws ConversionException
    {
        final List<Method> methods = RecordAccessorFallbackUtil.resolveAccessors(type);
        final Map<String, Method> methodMap = new HashMap<>(methods.size());
        for (final Method method : methods)
        {
            methodMap.put(method.getName(), method);
        }

        final Method[] accessors = new Method[componentNames.length];
        for (int index = 0; index < componentNames.length; index++)
        {
            final String componentName = componentNames[index];
            final Method accessor = methodMap.get(componentName);
            if (accessor == null)
            {
                throw new ConversionException(
                    "Record accessor not found for parameter '"
                        + componentName
                        + "' in "
                        + type.getName()
                );
            }
            if (!Modifier.isPublic(accessor.getModifiers()))
            {
                accessor.setAccessible(true);
            }
            accessors[index] = accessor;
        }
        return accessors;
    }

}
