package com.aktor.core;

import com.aktor.core.data.Relation;
import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.FieldResolver;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryFactoryLoader;
import com.aktor.core.model.RepositoryProvider;
import com.aktor.core.model.RelationProviderResolver;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RepositoryFactorySql
implements RepositoryFactory
{
    private final Connection connection;

    public RepositoryFactorySql(final Connection connection)
    {
        this.connection = Objects.requireNonNull(connection);
    }

    @Override
    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final RepositoryProvider provider,
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        return repository(provider, name, itemType, keyType, new RelationProviderResolver<>());
    }

    @Override
    public <Item extends Data<Key>, Key> Repository<Item, Key> repository(
        final RepositoryProvider provider,
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        if (Relation.class.isAssignableFrom(Objects.requireNonNull(itemType)))
        {
            return relationRepository(provider, name, itemType);
        }
        if (!Record.class.isAssignableFrom(itemType))
        {
            throw new IllegalArgumentException("SQL repository requires record type: " + itemType.getName());
        }
        @SuppressWarnings("unchecked")
        final Class<Item> recordType = (Class<Item>) itemType;
        return RepositorySql.of(
            connection,
            recordType,
            table(Objects.requireNonNull(provider).configuration(), Objects.requireNonNull(name)),
            "sqlite",
            relationProviderResolver
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <Item extends Data<Key>, Key> Repository<Item, Key> relationRepository(
        final RepositoryProvider provider,
        final String name,
        final Class<Item> itemType
    )
    {
        final String table = table(provider.configuration(), name);
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", "key");
        fields.put("mainKey", "main_key");
        fields.put("foreignKey", "foreign_key");
        final FieldResolver fieldResolver = FieldResolver.mapped((Class) Relation.class, fields);
        return (Repository<Item, Key>) RepositorySql.of(
            connection,
            (Class) Relation.class,
            table,
            "sqlite",
            fieldResolver,
            new RelationProviderResolver<>()
        );
    }

    private static String table(final Configuration configuration, final String name)
    {
        final Configuration entity = entity(configuration, name);
        final Configuration storage = entity.getConfiguration("storage");
        final String sqliteTable = storage.getString("table");
        if (sqliteTable != null && !sqliteTable.isBlank())
        {
            return sqliteTable;
        }
        final String table = entity.getString("table");
        if (table != null && !table.isBlank())
        {
            return table;
        }
        throw new IllegalArgumentException("No SQLite table configured for binding: " + name);
    }

    private static Configuration entity(final Configuration configuration, final String name)
    {
        final Configuration entities = configuration.getConfiguration("entity");
        if (entities.has(name))
        {
            return entities.getConfiguration(name);
        }
        return configuration.getConfiguration(name);
    }

    public static final class Loader
    implements RepositoryFactoryLoader
    {
        @Override
        public String kind()
        {
            return "sqlite";
        }

        @Override
        public RepositoryFactory load(final Environment environment)
        {
            return new RepositoryFactorySql(environment.require(Connection.class));
        }
    }
}
