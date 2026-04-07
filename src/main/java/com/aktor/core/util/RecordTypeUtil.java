package com.aktor.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class RecordTypeUtil
{
    private static final Method CLASS_IS_RECORD_METHOD = findMethod(Class.class, "isRecord");

    private static final Method CLASS_GET_RECORD_COMPONENTS_METHOD = findMethod(Class.class, "getRecordComponents");

    private static final Class<?> RECORD_CLASS = findClass("java.lang.Record");

    private static final Class<?> RECORD_COMPONENT_CLASS = findClass("java.lang.reflect.RecordComponent");

    private static final Method RECORD_COMPONENT_GET_NAME_METHOD =
        findMethod(RECORD_COMPONENT_CLASS, "getName");

    private static final Method RECORD_COMPONENT_GET_TYPE_METHOD =
        findMethod(RECORD_COMPONENT_CLASS, "getType");

    private static final Method RECORD_COMPONENT_GET_ACCESSOR_METHOD =
        findMethod(RECORD_COMPONENT_CLASS, "getAccessor");

    private RecordTypeUtil()
    {
        super();
    }

    public static boolean isRecordType(final Class<?> type)
    {
        final Class<?> safeType = Objects.requireNonNull(type);
        final Boolean recordMethodResult = invokeBoolean(CLASS_IS_RECORD_METHOD, safeType);
        if (Boolean.TRUE.equals(recordMethodResult))
        {
            return true;
        }
        if (RECORD_CLASS != null && RECORD_CLASS.isAssignableFrom(safeType))
        {
            return true;
        }
        return supportsAccessorFallback(safeType);
    }

    public static ComponentDescriptor[] getRecordComponents(final Class<?> type)
    {
        final Class<?> safeType = Objects.requireNonNull(type);
        if (CLASS_GET_RECORD_COMPONENTS_METHOD == null
            || RECORD_COMPONENT_GET_NAME_METHOD == null
            || RECORD_COMPONENT_GET_TYPE_METHOD == null
            || RECORD_COMPONENT_GET_ACCESSOR_METHOD == null)
        {
            return new ComponentDescriptor[0];
        }

        try
        {
            final Object result = CLASS_GET_RECORD_COMPONENTS_METHOD.invoke(safeType);
            if (!(result instanceof final Object[] components) || components.length == 0)
            {
                return new ComponentDescriptor[0];
            }

            return Arrays.stream(components)
                .map(RecordTypeUtil::toComponentDescriptor)
                .filter(Objects::nonNull)
                .toArray(ComponentDescriptor[]::new);
        }
        catch (final ReflectiveOperationException | RuntimeException ignored)
        {
            return new ComponentDescriptor[0];
        }
    }

    private static ComponentDescriptor toComponentDescriptor(final Object component)
    {
        try
        {
            final Object name = RECORD_COMPONENT_GET_NAME_METHOD.invoke(component);
            final Object type = RECORD_COMPONENT_GET_TYPE_METHOD.invoke(component);
            final Object accessor = RECORD_COMPONENT_GET_ACCESSOR_METHOD.invoke(component);
            if (name instanceof final String safeName
                && type instanceof final Class<?> safeType
                && accessor instanceof final Method safeAccessor)
            {
                return new ComponentDescriptor(safeName, safeType, safeAccessor);
            }
        }
        catch (final ReflectiveOperationException | RuntimeException ignored)
        {
        }
        return null;
    }

    private static Boolean invokeBoolean(final Method method, final Object target)
    {
        if (method == null)
        {
            return null;
        }
        try
        {
            final Object result = method.invoke(target);
            return result instanceof Boolean ? (Boolean) result : null;
        }
        catch (final ReflectiveOperationException | RuntimeException ignored)
        {
            return null;
        }
    }

    private static Method findMethod(final Class<?> type, final String name)
    {
        if (type == null)
        {
            return null;
        }
        try
        {
            return type.getMethod(name);
        }
        catch (final NoSuchMethodException ignored)
        {
            return null;
        }
    }

    private static Class<?> findClass(final String className)
    {
        try
        {
            return Class.forName(className);
        }
        catch (final ClassNotFoundException ignored)
        {
            return null;
        }
    }

    private static boolean supportsAccessorFallback(final Class<?> type)
    {
        final Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 0)
        {
            return false;
        }

        final List<Method> accessors = RecordAccessorFallbackUtil.resolveAccessors(type);
        if (accessors.isEmpty())
        {
            return false;
        }

        final Map<String, Method> accessorsByName = accessors.stream()
            .collect(Collectors.toMap(Method::getName, method -> method, (left, right) -> left));

        Constructor<?> bestConstructor = null;
        for (final Constructor<?> constructor : constructors)
        {
            if (bestConstructor == null
                || constructor.getParameterTypes().length > bestConstructor.getParameterTypes().length)
            {
                bestConstructor = constructor;
            }
        }
        if (bestConstructor == null)
        {
            return false;
        }

        final String[] componentNames = getFallbackComponentNames(type);
        if (componentNames.length != bestConstructor.getParameterTypes().length)
        {
            return false;
        }
        return Arrays.stream(componentNames)
            .allMatch(accessorsByName::containsKey);
    }

    public static String[] getFallbackComponentNames(final Class<?> type)
    {
        final Constructor<?>[] constructors = Objects.requireNonNull(type).getDeclaredConstructors();
        Constructor<?> bestConstructor = null;
        for (final Constructor<?> constructor : constructors)
        {
            if (bestConstructor == null
                || constructor.getParameterTypes().length > bestConstructor.getParameterTypes().length)
            {
                bestConstructor = constructor;
            }
        }
        if (bestConstructor != null)
        {
            final Parameter[] parameters = bestConstructor.getParameters();
            final boolean parameterNamesPresent = parameters.length > 0
                && Arrays.stream(parameters).allMatch(Parameter::isNamePresent);
            if (parameterNamesPresent)
            {
                return Arrays.stream(parameters)
                    .map(Parameter::getName)
                    .toArray(String[]::new);
            }
        }

        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .filter(field -> !field.isSynthetic())
            .map(Field::getName)
            .toArray(String[]::new);
    }

    public record ComponentDescriptor(String name, Class<?> type, Method accessor)
        {
            public ComponentDescriptor(final String name, final Class<?> type, final Method accessor)
            {
                this.name = Objects.requireNonNull(name);
                this.type = Objects.requireNonNull(type);
                this.accessor = Objects.requireNonNull(accessor);
            }
        }
}
