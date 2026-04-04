package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

public interface Repository<Item extends Data<Key>, Key>
extends Model
{
    Item get(final Key key) throws GetException;

    SearchResult<Item> search(SearchCriteria searchCriteria) throws SearchException;

    void save(final Item item) throws SaveException;

    void delete(final Item item) throws DeleteException;
}
