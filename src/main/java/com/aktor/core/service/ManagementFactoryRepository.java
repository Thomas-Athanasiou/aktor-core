package com.aktor.core.service;

import com.aktor.core.Data;
import com.aktor.core.Repository;
import com.aktor.core.data.Relation;
import com.aktor.core.model.Configuration;
import com.aktor.core.model.Environment;
import com.aktor.core.model.FactoryContext;
import com.aktor.core.model.ManagementFactory;
import com.aktor.core.model.ManagementFactoryLoader;
import com.aktor.core.model.ManagementProvider;
import com.aktor.core.model.ManagementRequest;
import com.aktor.core.model.RepositoryFactory;
import com.aktor.core.model.RepositoryRequest;
import com.aktor.core.model.RelationBinding;
import com.aktor.core.model.RelationCyclePolicy;
import com.aktor.core.model.RelationCardinalityPolicy;
import com.aktor.core.model.RelationDeletePolicy;
import com.aktor.core.model.RelationProcessor;
import com.aktor.core.model.RelationProviderResolver;
import com.aktor.core.model.RelationSavePolicy;
import com.aktor.core.model.RelationStoragePolicy;

import java.util.Objects;

public final class ManagementFactoryRepository
implements ManagementFactory
{
    @Override
    public <Item extends Data<Key>, Key> Management<Item, Key> createTyped(
        final FactoryContext context,
        final ManagementRequest<Item, Key> request
    )
    {
        final ManagementProvider provider = ManagementFactory.requireProvider(context);
        final RelationProviderResolver.Builder<Key> builder = RelationProviderResolver.builder();
        final Configuration relations = entity(provider.configuration(), request.name()).getConfiguration("relation");
        for (final String field : relations.keys())
        {
            builder.add(relationBinding(provider, request.name(), field, relations.getConfiguration(field)));
        }

        final RelationProviderResolver<Key> relationProviderResolver = builder.build();
        final RepositoryRequest<Item, Key> repositoryRequest = RepositoryFactory.request(
            Objects.requireNonNull(request.name()),
            Objects.requireNonNull(request.itemType()),
            Objects.requireNonNull(request.keyType()),
            relationProviderResolver
        );
        final Repository<Item, Key> repository = Objects.requireNonNull(provider).<Item, Key>repositories().instance(repositoryRequest);

        return new ManagementRepository<>(repository, new RelationProcessor<>(relationProviderResolver));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private RelationBinding relationBinding(
        final ManagementProvider provider,
        final String entityName,
        final String field,
        final Configuration relation
    )
    {
        final String targetName = requireConfiguration(relation, "target", "relation target");
        final Configuration targetEntity = entity(provider.configuration(), targetName);
        final Class<?> targetItemType = requireClass(targetEntity.getString("type"), targetName + ".type");
        final Class<?> targetKeyType = requireClass(targetEntity.getString("keyType"), targetName + ".keyType");
        final String relationRepositoryName = relationRepositoryName(entityName, field, relation);
        final String mainField = firstNonBlank(relation.getString("mainField"), "main_key");
        final String foreignField = firstNonBlank(relation.getString("foreignField"), "foreign_key");
        final RelationCardinalityPolicy cardinalityPolicy = enumValue(
            firstNonBlank(relation.getString("cardinalityPolicy"), relation.getString("integrityPolicy")),
            RelationCardinalityPolicy.ONE_TO_ONE
        );
        final RelationStoragePolicy storagePolicy = enumValue(
            relation.getString("storagePolicy"),
            RelationStoragePolicy.SEPARATE
        );
        final RelationCyclePolicy cyclePolicy = enumValue(
            relation.getString("cyclePolicy"),
            RelationCyclePolicy.REJECT
        );
        final RelationSavePolicy savePolicy = enumValue(relation.getString("savePolicy"), RelationSavePolicy.CASCADE);
        final RelationDeletePolicy deletePolicy = enumValue(relation.getString("deletePolicy"), RelationDeletePolicy.CASCADE);

        final Management<?, ?> targetManagement = provider.management(
            targetName,
            (Class) targetItemType.asSubclass(Data.class),
            (Class) targetKeyType
        );
        final Repository relationRepository = provider.<Relation, String>repositories().instance(
            RepositoryFactory.request(
                relationRepositoryName,
                Relation.class,
                String.class,
                new RelationProviderResolver<>()
            )
        );

        return new RelationBinding(
            field,
            (Class) targetItemType.asSubclass(Data.class),
            (Management) targetManagement,
            (Repository) relationRepository,
            Relation::new,
            mainField,
            foreignField,
            cardinalityPolicy,
            storagePolicy,
            cyclePolicy,
            savePolicy,
            deletePolicy
        );
    }

    private Configuration entity(final Configuration configuration, final String name)
    {
        final Configuration entities = configuration.getConfiguration("entity");
        if (entities.has(name))
        {
            return entities.getConfiguration(name);
        }
        return configuration.getConfiguration(name);
    }

    private static String relationRepositoryName(
        final String entityName,
        final String field,
        final Configuration relation
    )
    {
        return firstNonBlank(relation.getString("repository"), entityName + "." + field + ".relation");
    }

    private static String requireConfiguration(
        final Configuration configuration,
        final String key,
        final String label
    )
    {
        final String value = configuration.getString(key);
        if (value == null)
        {
            throw new IllegalArgumentException(label + " is required");
        }
        final String trimmed = value.trim();
        if (trimmed.isEmpty())
        {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }

    private static Class<?> requireClass(final String className, final String label)
    {
        try
        {
            return Class.forName(requireName(className, label));
        }
        catch (final ClassNotFoundException exception)
        {
            throw new IllegalArgumentException("Unknown class for " + label + ": " + className, exception);
        }
    }

    private static String requireName(final String value, final String label)
    {
        if (value == null)
        {
            throw new IllegalArgumentException(label + " is required");
        }
        final String trimmed = value.trim();
        if (trimmed.isEmpty())
        {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }

    private static String firstNonBlank(final String first, final String second)
    {
        if (first != null && !first.isBlank())
        {
            return first.trim();
        }
        if (second != null && !second.isBlank())
        {
            return second.trim();
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

    public static final class Loader
    implements ManagementFactoryLoader
    {
        @Override
        public String kind()
        {
            return "repository";
        }

        @Override
        public ManagementFactory load(final Environment environment)
        {
            return new ManagementFactoryRepository();
        }
    }
}
