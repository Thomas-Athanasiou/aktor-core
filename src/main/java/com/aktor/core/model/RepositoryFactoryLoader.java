package com.aktor.core.model;

import com.aktor.core.Data;

public interface RepositoryFactoryLoader<Item extends Data<Key>, Key>
extends Loader<RepositoryFactory<Item, Key>>
{
    String kind();
}
