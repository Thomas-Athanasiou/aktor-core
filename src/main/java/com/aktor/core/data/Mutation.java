package com.aktor.core.data;

import com.aktor.core.Data;

import java.util.Objects;

public record Mutation<Key, Item> (Key key, Item from, Item to)
implements Data<Key>
{
    public Mutation
    {
        Objects.requireNonNull(key);
    }
}
