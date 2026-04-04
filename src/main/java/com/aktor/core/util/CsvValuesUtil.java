package com.aktor.core.util;

import java.util.regex.Pattern;

public final class CsvValuesUtil
{
    private static final String[] STRINGS = new String[0];

    private static final Pattern CSV_SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");

    private CsvValuesUtil()
    {
        super();
    }

    public static String[] split(final String commaSeparated)
    {
        return (commaSeparated == null || commaSeparated.isBlank()) ? STRINGS : CSV_SPLIT_PATTERN.split(commaSeparated);
    }
}

