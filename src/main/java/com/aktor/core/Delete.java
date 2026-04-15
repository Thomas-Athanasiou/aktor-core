package com.aktor.core;

import com.aktor.core.exception.DeleteException;

public interface Delete<Item extends Data<Key>, Key>
{
    void delete(final Item item) throws DeleteException;
}
