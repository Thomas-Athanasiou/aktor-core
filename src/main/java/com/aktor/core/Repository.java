package com.aktor.core;

public interface Repository<Item extends Data<Key>, Key>
extends Search<Item, Key>, Registry<Item, Key>
{
}
