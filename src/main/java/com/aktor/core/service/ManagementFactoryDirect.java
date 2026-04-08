package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.model.Environment;
import com.aktor.core.model.ManagementFactory;
import com.aktor.core.model.ManagementFactoryLoader;
import com.aktor.core.model.ManagementProvider;

import java.util.Objects;

public final class ManagementFactoryDirect
implements ManagementFactory
{
    @Override
    public <Item extends Data<Key>, Key> Management<Item, Key> management(
        final ManagementProvider provider,
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        final Repository<Item, Key> repository = Objects.requireNonNull(provider).repository(
            Objects.requireNonNull(name),
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(keyType)
        );
        return ManagementRepository.noRelations(repository);
    }

    public static final class Loader
    implements ManagementFactoryLoader
    {
        @Override
        public String kind()
        {
            return "direct";
        }

        @Override
        public ManagementFactory load(final Environment environment)
        {
            return new ManagementFactoryDirect();
        }
    }
}
