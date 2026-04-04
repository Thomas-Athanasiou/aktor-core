package com.aktor.core.model;

public interface Configuration
{
    String getString(String path);

    String[] getStrings(String path);

    Long getLong(String path);

    Long[] getLongs(String path);

    Integer getInteger(String path);

    Integer[] getIntegers(String path);

    Boolean getBoolean(String path);

    Boolean[] getBooleans(String path);
}
