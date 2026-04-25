package com.aktor.core.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Function;

public class ConfigurationProperties
implements Configuration
{
    private final Properties properties;

    public ConfigurationProperties(final String filePath)
    {
        super();
        final Properties properties = new Properties();
        try (final FileInputStream stream = new FileInputStream(filePath))
        {
            properties.load(stream);
        }
        catch (final IOException exception)
        {
            throw new RuntimeException(exception);
        }
        this.properties = properties;
    }

    public ConfigurationProperties(final Properties properties)
    {
        this.properties = new Properties();
        this.properties.putAll(properties);
    }

    @Override
    public String getString(final String key)
    {
        return properties.getProperty(key);
    }

    @Override
    public boolean has(final String key)
    {
        if (properties.containsKey(key))
        {
            return true;
        }
        final String prefix = key + ".";
        for (final String candidate : properties.stringPropertyNames())
        {
            if (candidate.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] keys()
    {
        return properties.stringPropertyNames().toArray(new String[0]);
    }

    @Override
    public Long getLong(final String key)
    {
        return readValue(key, Long::parseLong);
    }

    @Override
    public Integer getInteger(final String key)
    {
        return readValue(key, Integer::parseInt);
    }

    @Override
    public Boolean getBoolean(final String key)
    {
        return readValue(key, Boolean::parseBoolean);
    }

    @Override
    public Configuration getConfiguration(final String key)
    {
        return new ConfigurationSection(this, key);
    }

    private <T> T readValue(final String key, final Function<String, T> parser)
    {
        return parse(getString(key), parser);
    }

    private static <T> T parse(final String value, final Function<String, T> parser)
    {
        return value == null ? null : parser.apply(value.trim());
    }
}
