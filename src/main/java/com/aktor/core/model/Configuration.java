package com.aktor.core.model;

public interface Configuration
{
    String getString(String key);

    default String optString(final String key, final String defaultValue)
    {
        final String value = getString(key);
        return value == null ? defaultValue : value;
    }

    default String requireString(final String key, final String label)
    {
        final String value = getString(key);
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    boolean has(String key);

    String[] keys();

    Long getLong(String key);

    default Long optLong(final String key, final Long defaultValue)
    {
        final Long value = getLong(key);
        return value == null ? defaultValue : value;
    }

    Integer getInteger(String key);

    default Integer optInteger(final String key, final Integer defaultValue)
    {
        final Integer value = getInteger(key);
        return value == null ? defaultValue : value;
    }

    Boolean getBoolean(String key);

    default Boolean optBoolean(final String key, final Boolean defaultValue)
    {
        final Boolean value = getBoolean(key);
        return value == null ? defaultValue : value;
    }

    Configuration getConfiguration(String key);

    default Configuration optConfiguration(final String key, final Configuration defaultValue)
    {
        return has(key) ? getConfiguration(key) : defaultValue;
    }

    default Configuration entity(final String name)
    {
        return getConfiguration("entity").has(name)
            ? getConfiguration("entity").getConfiguration(name)
            : getConfiguration(name);
    }

    default Configuration storage(final String entityName)
    {
        return entity(entityName).getConfiguration("storage");
    }
}
