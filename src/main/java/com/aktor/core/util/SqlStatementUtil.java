package com.aktor.core.util;

import com.aktor.core.Value;

public final class SqlStatementUtil
{
    private SqlStatementUtil()
    {
        super();
    }

    public static String columns(final Value[] values, final String start, final String end)
    {
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.length; index++)
        {
            if (index > 0)
            {
                builder.append(", ");
            }
            builder.append(start).append(values[index].field()).append(end);
        }
        return builder.toString();
    }

    public static String placeholders(final int count)
    {
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++)
        {
            if (index > 0)
            {
                builder.append(", ");
            }
            builder.append('?');
        }
        return builder.toString();
    }

    public static String assignments(final Value[] values, final String start, final String end)
    {
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.length; index++)
        {
            if (index > 0)
            {
                builder.append(", ");
            }
            builder.append(start).append(values[index].field()).append(end).append(" = ?");
        }
        return builder.toString();
    }
}
