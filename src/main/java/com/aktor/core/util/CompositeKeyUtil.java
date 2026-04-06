package com.aktor.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.Base64;

public final class CompositeKeyUtil
{
    private static final String SEPARATOR = ":";

    private CompositeKeyUtil()
    {
    }

    public static String encode(final String[] parts)
    {
        final StringBuilder stringBuilder = new StringBuilder();
        IntStream.range(0, parts.length).forEachOrdered(
            index -> {
                stringBuilder.append(
                    Base64.getUrlEncoder().withoutPadding().encodeToString(parts[index].getBytes(StandardCharsets.UTF_8))
                );
                if (index < parts.length - 1)
                {
                    stringBuilder.append(SEPARATOR);
                }
            }
        );
        return stringBuilder.toString();
    }

    public static String[] decode(final String key)
    {
        final String[] parts = key.split(SEPARATOR);
        return Arrays.stream(parts)
            .map(part -> new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8))
            .toArray(String[]::new);
    }
}
