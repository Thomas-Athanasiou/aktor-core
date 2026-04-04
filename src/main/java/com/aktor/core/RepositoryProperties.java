package com.aktor.core;

import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.CollectionProcessor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class RepositoryProperties<Item extends Data<Key>, Key>
extends RepositorySerialized<Item, Key>
{
    protected final File file;

    protected final Properties properties;

    private final Object ioLock = new Object();

    private boolean loaded = false;

    public RepositoryProperties(
        final Converter<Item, String> serializer,
        final Converter<String, Item> deserializer,
        final CollectionProcessor<Item, Key> processor,
        final File file,
        final Properties properties
    )
    {
        super(serializer, deserializer, processor);
        this.file = Objects.requireNonNull(file);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    protected final List<String> getBatch(final int from, final int to) throws SearchException
    {
        synchronized (ioLock)
        {
            try
            {
                loadIfNecessary();
            }
            catch (final IOException exception)
            {
                throw new SearchException(exception);
            }
            return snapshotData(properties.stringPropertyNames(), from, to, properties::getProperty);
        }
    }

    @Override
    protected final List<String> getAllData() throws SearchException
    {
        synchronized (ioLock)
        {
            try
            {
                loadIfNecessary();
            }
            catch (final IOException exception)
            {
                throw new SearchException(exception);
            }
            return snapshotData(properties.stringPropertyNames(), properties::getProperty);
        }
    }

    @Override
    protected final String getByPath(final String path) throws GetException
    {
        synchronized (ioLock)
        {
            try
            {
                loadIfNecessary();
            }
            catch (final IOException exception)
            {
                throw new GetException(exception);
            }
            return properties.getProperty(path);
        }
    }

    @Override
    protected void assignToPath(final String path, final String data) throws SaveException
    {
        synchronized (ioLock)
        {
            try
            {
                loadIfNecessary();
                properties.setProperty(path, data);
                persist();
            }
            catch (final IOException exception)
            {
                throw new SaveException(exception);
            }
        }
    }

    @Override
    protected final void removeFromPath(final String path) throws DeleteException
    {
        synchronized (ioLock)
        {
            try
            {
                loadIfNecessary();
                properties.remove(path);
                persist();
            }
            catch (final IOException ioException)
            {
                throw new DeleteException(ioException);
            }
        }
    }

    private void loadIfNecessary() throws IOException
    {
        if (!loaded)
        {
            if(ensureFileExistsAndReadable(file))
            {
                try (final InputStream inputStream = new FileInputStream(file))
                {
                    properties.load(inputStream);
                }
            }
            else
            {
                throw new FileNotFoundException("Could not create or read File");
            }
            loaded = true;
        }
    }

    private void persist() throws IOException
    {
        if(ensureFileExistsAndWritable(file))
        {
            try (final OutputStream outputStream = new FileOutputStream(file))
            {
                properties.store(outputStream, null);
            }
        }
        else
        {
            throw new FileNotFoundException("Could not create output stream.");
        }
    }

    private boolean ensureFileExistsAndReadable(final File file) throws IOException
    {
        return ensureFileExists(file) && file.canRead();
    }

    private boolean ensureFileExistsAndWritable(final File file) throws IOException
    {
        return ensureFileExists(file) && file.canWrite();
    }

    private boolean ensureFileExists(final File file) throws IOException
    {
        final File parent = file.getParentFile();
        return (parent == null || parent.exists() || parent.mkdirs()) && (file.exists() || file.createNewFile());
    }
}
