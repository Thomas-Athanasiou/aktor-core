package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.model.Environment;
import com.aktor.core.model.FactoryContext;
import com.aktor.core.model.ManagementFactory;
import com.aktor.core.model.ManagementFactoryLoader;
import com.aktor.core.model.ManagementProvider;
import com.aktor.core.model.ManagementRequest;

import java.util.Objects;

public final class ManagementFactoryDirect
implements ManagementFactory
{
    @Override
    public <Item extends Data<Key>, Key> Management<Item, Key> createTyped(
        final FactoryContext context,
        final ManagementRequest<Item, Key> request
    )
    {
        final ManagementProvider provider = ManagementFactory.requireProvider(context);
        final Repository<Item, Key> repository = Objects.requireNonNull(provider).repository(
            Objects.requireNonNull(request.name()),
            Objects.requireNonNull(request.itemType()),
            Objects.requireNonNull(request.keyType())
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
