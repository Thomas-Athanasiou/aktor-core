package com.aktor.core.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class ConfigurationSection
implements Configuration
{
    private final Configuration configuration;
    private final String key;

    public ConfigurationSection(final Configuration configuration, final String key)
    {
        this.configuration = Objects.requireNonNull(configuration);
        this.key = normalizeKey(key);
    }

    @Override
    public String getString(final String nestedKey)
    {
        return configuration.getString(resolveKey(nestedKey));
    }

    @Override
    public boolean has(final String nestedKey)
    {
        final String resolvedKey = resolveKey(nestedKey);
        if (configuration.has(resolvedKey))
        {
            return true;
        }
        final String prefix = resolvedKey + ".";
        for (final String candidate : configuration.keys())
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
        final Set<String> result = new LinkedHashSet<>();
        final String prefix = key + ".";
        for (final String candidate : configuration.keys())
        {
            if (candidate.startsWith(prefix))
            {
                final String remainder = candidate.substring(prefix.length());
                if (!remainder.isBlank())
                {
                    final int dot = remainder.indexOf('.');
                    result.add(dot < 0 ? remainder : remainder.substring(0, dot));
                }
            }
        }
        return result.toArray(new String[0]);
    }

    @Override
    public Long getLong(final String nestedKey)
    {
        return configuration.getLong(resolveKey(nestedKey));
    }

    @Override
    public Integer getInteger(final String nestedKey)
    {
        return configuration.getInteger(resolveKey(nestedKey));
    }

    @Override
    public Boolean getBoolean(final String nestedKey)
    {
        return configuration.getBoolean(resolveKey(nestedKey));
    }

    @Override
    public Configuration getConfiguration(final String nestedKey)
    {
        return new ConfigurationSection(configuration, resolveKey(nestedKey));
    }

    private String resolveKey(final String nestedKey)
    {
        final String value = normalizeKey(nestedKey);
        return key.isBlank() ? value : key + "." + value;
    }

    private static String normalizeKey(final String value)
    {
        final String key = Objects.requireNonNull(value);
        if (key.isBlank())
        {
            throw new IllegalArgumentException("key cannot be blank");
        }
        return key.trim();
    }
}
