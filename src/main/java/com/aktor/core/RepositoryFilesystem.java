package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.CollectionProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class RepositoryFilesystem<Item extends Data<Key>, Key>
extends RepositorySerialized<Item, Key>
{
    private final Path root;
    private final String prefix;
    private final String suffix;
    private final Object ioLock = new Object();

    public RepositoryFilesystem(
        final Converter<Item, String> serializer,
        final Converter<String, Item> deserializer,
        final CollectionProcessor<Item, Key> processor,
        final Path root,
        final String prefix,
        final String suffix
    )
    {
        super(serializer, deserializer, processor);
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        this.prefix = prefix == null ? "" : prefix;
        this.suffix = suffix == null ? "" : suffix;
    }

    public RepositoryFilesystem(
        final Converter<Item, String> serializer,
        final Converter<String, Item> deserializer,
        final CollectionProcessor<Item, Key> processor,
        final Path root,
        final String suffix
    )
    {
        this(serializer, deserializer, processor, root, "", suffix);
    }

    @Override
    protected String getByPath(final String path) throws GetException
    {
        synchronized (ioLock)
        {
            final Path file = file(path);
            if (!Files.isRegularFile(file))
            {
                return null;
            }
            try
            {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
            catch (final IOException exception)
            {
                throw new GetException(exception);
            }
        }
    }

    @Override
    protected List<String> getBatch(final int from, final int to) throws SearchException
    {
        synchronized (ioLock)
        {
            try
            {
                return snapshotData(keys(), from, to, key -> {
                    try
                    {
                        return Files.readString(file(key), StandardCharsets.UTF_8);
                    }
                    catch (final IOException exception)
                    {
                        throw new FilesystemRuntimeException(exception);
                    }
                });
            }
            catch (final FilesystemRuntimeException exception)
            {
                throw new SearchException(exception.getCause());
            }
        }
    }

    @Override
    protected List<String> getAllData() throws SearchException
    {
        synchronized (ioLock)
        {
            try
            {
                return snapshotData(
                    keys(),
                    key -> {
                        try
                        {
                            return Files.readString(file(key), StandardCharsets.UTF_8);
                        }
                        catch (final IOException exception)
                        {
                            throw new FilesystemRuntimeException(exception);
                        }
                    }
                );
            }
            catch (final FilesystemRuntimeException exception)
            {
                throw new SearchException(exception.getCause());
            }
        }
    }

    @Override
    protected void assignToPath(final String path, final String data) throws SaveException
    {
        synchronized (ioLock)
        {
            final Path file = file(path);
            try
            {
                final Path parent = file.getParent();
                if (parent != null)
                {
                    Files.createDirectories(parent);
                }
                Files.writeString(file, data == null ? "" : data, StandardCharsets.UTF_8);
            }
            catch (final IOException exception)
            {
                throw new SaveException(exception);
            }
        }
    }

    @Override
    protected void removeFromPath(final String path) throws DeleteException
    {
        synchronized (ioLock)
        {
            try
            {
                Files.deleteIfExists(file(path));
            }
            catch (final IOException exception)
            {
                throw new DeleteException(exception);
            }
        }
    }

    private List<String> keys() throws SearchException
    {
        if (!Files.exists(root))
        {
            return List.of();
        }
        if (!Files.isDirectory(root))
        {
            throw new SearchException("Filesystem repository root is not a directory: " + root);
        }

        final List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root))
        {
            stream
                .filter(Files::isRegularFile)
                .map(root::relativize)
                .map(Path::toString)
                .map(path -> path.replace('\\', '/'))
                .filter(this::matches)
                .map(this::key)
                .forEach(result::add);
        }
        catch (final IOException exception)
        {
            throw new SearchException(exception);
        }
        return result;
    }

    private Path file(final String key)
    {
        final String safeKey = Objects.requireNonNull(key, "key").replace('\\', '/');
        final Path file = root.resolve(prefix + safeKey + suffix).normalize();
        if (!file.startsWith(root))
        {
            throw new IllegalArgumentException("Repository key escapes filesystem root: " + key);
        }
        return file;
    }

    private boolean matches(final String path)
    {
        return path.startsWith(prefix) && (suffix.isBlank() || path.endsWith(suffix));
    }

    private String key(final String path)
    {
        String result = path;
        if (!prefix.isBlank())
        {
            result = result.substring(prefix.length());
        }
        if (!suffix.isBlank())
        {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }

    private static final class FilesystemRuntimeException
    extends RuntimeException
    {
        private FilesystemRuntimeException(final IOException cause)
        {
            super(cause);
        }

        @Override
        public synchronized IOException getCause()
        {
            return (IOException) super.getCause();
        }
    }
}
