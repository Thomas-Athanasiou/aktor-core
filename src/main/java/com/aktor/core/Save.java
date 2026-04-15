package com.aktor.core;

import com.aktor.core.exception.SaveException;

public interface Save<Item extends Data<Key>, Key>
{
    void save(final Item item) throws SaveException;
}
