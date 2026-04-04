package com.aktor.core.model;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RecordAccessorFallbackUtil
{
    private RecordAccessorFallbackUtil()
    {
        super();
    }

    public static List<Method> resolveAccessors(final Class<?> type)
    {
        final Method[] methods = type.getDeclaredMethods();
        final List<Method> accessorMethods = new ArrayList<>(methods.length);
        for (final Method method : methods)
        {
            if (isAccessorMethod(method))
            {
                accessorMethods.add(method);
            }
        }
        accessorMethods.sort(Comparator.comparing(Method::getName));
        return accessorMethods;
    }

    private static boolean isAccessorMethod(final Method method)
    {
        final String name = method.getName();
        return method.getParameterTypes().length == 0
            && !Modifier.isStatic(method.getModifiers())
            && !method.isSynthetic()
            && !method.isBridge()
            && method.getReturnType() != Void.TYPE
            && !"toString".equals(name)
            && !"hashCode".equals(name)
            && !"getClass".equals(name);
    }
}
