package com.aktor.core;

public interface Registry<Item extends Data<Key>, Key>
extends Get<Item, Key>, Save<Item, Key>, Delete<Item, Key>
{
}
