package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.service.ManagementFactoryDirect;
import com.aktor.core.service.ManagementFactoryRepository;
import com.aktor.core.service.Management;
import com.aktor.core.service.ManagementDelegate;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ManagementProvider
extends Provider<ManagementFactory>
{
    private final RepositoryProvider repositories;
    private final Map<String, Management<?, ?>> managements = new ConcurrentHashMap<>();
    private final Map<String, ManagementDelegate<?, ?>> pendingManagements = new ConcurrentHashMap<>();

    public ManagementProvider(final RepositoryProvider repositories)
    {
        super(
            Objects.requireNonNull(repositories).configuration(),
            ManagementFactory.class,
            combineLoaders(
                ServiceLoader.load(ManagementFactoryLoader.class),
                List.of(
                    new ManagementFactoryDirect.Loader(),
                    new ManagementFactoryRepository.Loader()
                )
            ),
            repositories
        );
        this.repositories = repositories;
    }

    public static ManagementProvider of(final RepositoryProvider repositories)
    {
        return new ManagementProvider(repositories);
    }

    public Configuration configuration()
    {
        return super.configuration();
    }

    public Environment environment()
    {
        return super.environment();
    }

    public <Dependency> Dependency require(final Class<Dependency> type)
    {
        return repositories.require(type);
    }

    public RepositoryProvider repositories()
    {
        return repositories;
    }

    public <Item extends Data<Key>, Key> Management<Item, Key> management(
        final String name,
        final Class<Item> itemType,
        final Class<Key> keyType
    )
    {
        final String safeName = Objects.requireNonNull(name);
        final Management<?, ?> existing = managements.get(safeName);
        if (existing != null)
        {
            return cast(existing);
        }
        final ManagementDelegate<?, ?> pending = pendingManagements.get(safeName);
        if (pending != null)
        {
            return cast(pending);
        }

        final ManagementDelegate<Item, Key> delegate = new ManagementDelegate<>();
        pendingManagements.put(safeName, delegate);
        try
        {
            final ManagementRequest<Item, Key> request = ManagementFactory.request(
                safeName,
                Objects.requireNonNull(itemType),
                Objects.requireNonNull(keyType)
            );
            final Management<Item, Key> created = super.instance(
                request.name(),
                request,
                (instanceName, context, safeRequest) -> ManagementFactory.of(this, instanceName).createTyped(context, safeRequest)
            );
            delegate.setDelegate(created);
            managements.put(safeName, created);
            return created;
        }
        finally
        {
            pendingManagements.remove(safeName);
        }
    }

    public Management<?, ?> management(final String name)
    {
        final Configuration entity = entity(configuration(), Objects.requireNonNull(name));
        final Class<?> itemType = requireClass(entity.getString("type"), name + ".type");
        final Class<?> keyType = requireClass(entity.getString("keyType"), name + ".keyType");
        return management(
            name,
            dataClass(itemType),
            classOf(keyType)
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

    @SuppressWarnings("unchecked")
    private static <Type> Class<Type> classOf(final Class<?> type)
    {
        return (Class<Type>) type;
    }

    @SuppressWarnings("unchecked")
    private static <Type extends Data<?>> Class<Type> dataClass(final Class<?> type)
    {
        return (Class<Type>) type.asSubclass(Data.class);
    }

    @SuppressWarnings("unchecked")
    private static <Item extends Data<Key>, Key> Management<Item, Key> cast(final Management<?, ?> management)
    {
        return (Management<Item, Key>) management;
    }
}
