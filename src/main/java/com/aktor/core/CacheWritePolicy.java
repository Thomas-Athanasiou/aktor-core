package com.aktor.core;

import java.util.Objects;

public final class CacheWritePolicy
{
    public static final CacheWritePolicy NONE = new CacheWritePolicy(0);

    public static final CacheWritePolicy FIRST_ONLY = new CacheWritePolicy(1);

    public static final CacheWritePolicy ALL = new CacheWritePolicy(Integer.MAX_VALUE);

    private final int writeSourceCount;

    private CacheWritePolicy(final int writeSourceCount)
    {
        super();
        this.writeSourceCount = Math.max(0, writeSourceCount);
    }

    public static CacheWritePolicy firstN(final int count)
    {
        final CacheWritePolicy policy;
        if (count <= 0)
        {
            policy = NONE;
        }
        else if (count == 1)
        {
            policy = FIRST_ONLY;
        }
        else if (count == Integer.MAX_VALUE)
        {
            policy = ALL;
        }
        else
        {
            policy = new CacheWritePolicy(count);
        }
        return policy;
    }

    public int writeSourceCount()
    {
        return writeSourceCount;
    }

    @Override
    public boolean equals(final Object object)
    {
        final boolean result;
        if (this == object)
        {
            result = true;
        }
        else if (object instanceof final CacheWritePolicy that)
        {
            result = writeSourceCount == that.writeSourceCount;
        }
        else
        {
            result = false;
        }
        return result;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(writeSourceCount);
    }
}
