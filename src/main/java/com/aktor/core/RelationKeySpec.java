package com.aktor.core;

import com.aktor.core.data.Relation;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.util.CompositeKeyUtil;

import java.util.Arrays;
import java.util.Objects;

final class RelationKeySpec
{
    static final String MAIN = "main_key";

    static final String FOREIGN = "foreign_key";

    private static final String[] KEY_FIELDS = new String[] {MAIN, FOREIGN};

    private RelationKeySpec()
    {
        super();
    }

    static boolean isRelationType(final Class<?> type)
    {
        return Relation.class.isAssignableFrom(Objects.requireNonNull(type));
    }

    static String[] keyFields()
    {
        return Arrays.copyOf(KEY_FIELDS, KEY_FIELDS.length);
    }

    static Object[] decodeKey(final Object key) throws ConversionException
    {
        if (key == null)
        {
            throw new ConversionException("Relation key cannot be null");
        }

        final String[] parts = CompositeKeyUtil.decode(String.valueOf(key));
        if (parts.length != KEY_FIELDS.length)
        {
            throw new ConversionException("Invalid relation key: " + key);
        }
        return Arrays.copyOf(parts, parts.length, Object[].class);
    }
}
