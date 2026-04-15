package com.aktor.core;

import com.aktor.core.exception.GetException;

public interface Get<Item extends Data<Key>, Key>
{
    Item get(final Key key) throws GetException;
}
