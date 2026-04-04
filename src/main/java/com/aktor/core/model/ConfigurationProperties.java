package com.aktor.core.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

public class ConfigurationProperties
implements Configuration
{
    private static final String[] STRINGS = new String[0];

    private static final Pattern PATTERN = Pattern.compile("[,;]");

    private final Properties properties;
    private final Map<String, String[]> stringValuesCache = new ConcurrentHashMap<>();

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

    @Override
    public String getString(final String path)
    {
        return properties.getProperty(path);
    }

    @Override
    public String[] getStrings(final String path)
    {
        final String[] values = stringValuesCache.computeIfAbsent(path, this::loadStrings);
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public Long getLong(final String path)
    {
        return readValue(path, Long::parseLong);
    }

    @Override
    public Long[] getLongs(final String path)
    {
        return readValues(path, Long::parseLong, Long[]::new);
    }

    @Override
    public Integer getInteger(final String path)
    {
        return readValue(path, Integer::parseInt);
    }

    @Override
    public Integer[] getIntegers(final String path)
    {
        return readValues(path, Integer::parseInt, Integer[]::new);
    }

    @Override
    public Boolean getBoolean(final String path)
    {
        return readValue(path, Boolean::parseBoolean);
    }

    @Override
    public Boolean[] getBooleans(final String path)
    {
        return readValues(path, Boolean::parseBoolean, Boolean[]::new);
    }

    private static String[] split(final CharSequence value)
    {
        return PATTERN.split(value);
    }

    private String[] loadStrings(final String path)
    {
        final String value = getString(path);
        if (value == null)
        {
            return STRINGS;
        }

        final String[] parts = split(value);
        final List<String> result = new ArrayList<>(parts.length);
        for (final String part : parts)
        {
            final String trimmed = part.trim();
            if (!trimmed.isEmpty())
            {
                result.add(trimmed);
            }
        }
        return result.toArray(STRINGS);
    }

    private <T> T readValue(final String path, final Function<String, T> parser)
    {
        return parse(getString(path), parser);
    }

    private <T> T[] readValues(
        final String path,
        final Function<String, T> parser,
        final IntFunction<T[]> arrayFactory
    )
    {
        final String[] values = getStrings(path);
        final T[] result = arrayFactory.apply(values.length);
        for (int index = 0; index < values.length; index++)
        {
            result[index] = parse(values[index], parser);
        }
        return result;
    }

    private static <T> T parse(final String value, final Function<String, T> parser)
    {
        return value == null ? null : parser.apply(value.trim());
    }
}
