package com.aktor.core.service;

import com.aktor.core.*;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;

public interface Management<Item extends Data<Key>, Key>
extends Service
{
    Item get(final Key key) throws GetException;

    SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException;

    void delete(final Item item) throws DeleteException;

    void save(final Item item) throws SaveException;

    boolean exists(final Key key);
}
