package com.aktor.core.model;

public interface Configuration
{
    String getString(String key);

    boolean has(String key);

    String[] keys();

    Long getLong(String key);

    Integer getInteger(String key);

    Boolean getBoolean(String key);

    Configuration getConfiguration(String key);
}
