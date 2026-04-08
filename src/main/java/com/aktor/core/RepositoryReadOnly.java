package com.aktor.core;

import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryFactoryLoader;
import com.aktor.core.model.RepositoryProvider;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.SaveException;

import java.util.Objects;

public class RepositoryReadOnly<Item extends Data<Key>, Key>
extends RepositoryWrapper<Item, Key>
{
    private final RepositoryReadOnlyMode mode;

    public RepositoryReadOnly(
        final Repository<Item, Key> repository,
        final RepositoryReadOnlyMode mode
    )
    {
        super(repository);
        this.mode = Objects.requireNonNull(mode);
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        if (mode == RepositoryReadOnlyMode.THROW)
        {
            throw new SaveException("Repository is read-only.");
        }
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        if (mode == RepositoryReadOnlyMode.THROW)
        {
            throw new DeleteException("Repository is read-only.");
        }
    }

    public static final class Factory implements RepositoryFactory
    {
        @Override
        public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
            final RepositoryProvider provider,
            final String name,
            final Class<Item> itemType,
            final Class<Key> keyType
        )
        {
            return repository(provider, name, itemType, keyType, new com.aktor.core.model.RelationProviderResolver<>());
        }

        @Override
        public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
            final RepositoryProvider provider,
            final String name,
            final Class<Item> itemType,
            final Class<Key> keyType,
            final com.aktor.core.model.RelationProviderResolver<Key> relationProviderResolver
        )
        {
            final Configuration wrapper = wrapper(provider.configuration(), name);
            final String source = firstNonBlank(
                singleSource(wrapper.getConfiguration("sources")),
                wrapper.getString("source"),
                wrapper.getString("repository")
            );
            if (source == null || source.isBlank())
            {
                throw new IllegalArgumentException("Wrapper source is required for repository: " + name);
            }
            final RepositoryReadOnlyMode mode = enumValue(wrapper.getString("mode"), RepositoryReadOnlyMode.THROW);
            return new RepositoryReadOnly<>(provider.repository(source, itemType, keyType, relationProviderResolver), mode);
        }

        private static Configuration wrapper(final Configuration configuration, final String name)
        {
            final Configuration entities = configuration.getConfiguration("entity");
            if (entities.has(name))
            {
                return entities.getConfiguration(name).getConfiguration("wrapper");
            }
            return configuration.getConfiguration(name).getConfiguration("wrapper");
        }

        private static String singleSource(final Configuration sources)
        {
            final String[] keys = sources.keys();
            return keys.length == 0 ? null : keys[0];
        }

        private static String firstNonBlank(final String first, final String second, final String third)
        {
            if (first != null && !first.isBlank())
            {
                return first.trim();
            }
            if (second != null && !second.isBlank())
            {
                return second.trim();
            }
            if (third != null && !third.isBlank())
            {
                return third.trim();
            }
            return null;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static <Type extends Enum<Type>> Type enumValue(final String value, final Type defaultValue)
        {
            if (value == null || value.isBlank())
            {
                return defaultValue;
            }
            return (Type) Enum.valueOf((Class) defaultValue.getDeclaringClass(), value.trim());
        }
    }

    public static final class Loader
    implements RepositoryFactoryLoader
    {
        @Override
        public String kind()
        {
            return "readonly";
        }

        @Override
        public RepositoryFactory load(final Environment environment)
        {
            return new Factory();
        }
    }
}
