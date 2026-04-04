package com.aktor.core;

import java.util.Objects;

public record Value(String field, String value)
{
    public Value
    {
        Objects.requireNonNull(field);
    }
}
