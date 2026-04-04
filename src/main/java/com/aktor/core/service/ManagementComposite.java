package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;

import java.util.List;
import java.util.Objects;

public abstract class ManagementComposite<Item extends Data<Key>, Key>
implements Management<Item, Key>
{
    protected final List<Management<Item, Key>> managementList;

    protected ManagementComposite(final List<Management<Item, Key>> managementList)
    {
        super();
        this.managementList = List.copyOf(Objects.requireNonNull(managementList));
    }

    @Override
    public final void save(final Item item) throws SaveException
    {
        for (final Management<Item, Key> management : managementList)
        {
            management.save(item);
        }
    }

    @Override
    public final void delete(final Item item) throws DeleteException
    {
        for (final Management<Item, Key> management : managementList)
        {
            management.delete(item);
        }
    }

    @Override
    public final boolean exists(final Key key)
    {
        for (final Management<Item, Key> management : managementList)
        {
            if (management.exists(key))
            {
                return true;
            }
        }
        return false;
    }
}
