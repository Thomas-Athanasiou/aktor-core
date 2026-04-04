package com.aktor.core;

import java.util.Objects;

public record SortOrder(String field, boolean direction)
{
    public SortOrder
    {
        Objects.requireNonNull(field);
    }
}
