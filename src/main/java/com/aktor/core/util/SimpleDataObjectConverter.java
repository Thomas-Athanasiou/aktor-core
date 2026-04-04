package com.aktor.core.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class SimpleDataObjectConverter
{
    private static final Map<Class<?>, TypeSupport> TYPE_SUPPORT = createTypeSupport();

    private static final Pattern PATTERN = Pattern.compile("([a-z])([A-Z])");

    private SimpleDataObjectConverter()
    {
    }

    public static String camelToSnake(final CharSequence text)
    {
        return PATTERN.matcher(text).replaceAll("$1_$2").toLowerCase();
    }

    public static boolean isScalar(final Class<?> target)
    {
        return TYPE_SUPPORT.containsKey(target);
    }

    public static boolean isPersistableType(final Class<?> target)
    {
        return isScalar(target) || Enum.class.isAssignableFrom(target);
    }

    public static Object convert(final Class<?> target, final String input) throws IllegalArgumentException
    {
        if(!isScalar(target))
        {
            throw new IllegalArgumentException("Inconvertible type provided");
        }
        return TYPE_SUPPORT.get(target).parser().apply(input);
    }

    public static String objectToString(final Object object)
    {
        final String result;
        if (object instanceof final Enum<?> enumValue)
        {
            result = enumValue.name();
        }
        else
        {
            result = object == null ? null : object.toString();
        }
        return result == null || result.isBlank() ? null : result;
    }

    public static String sqlType(final Class<?> type)
    {
        final TypeSupport support = TYPE_SUPPORT.get(type);
        return support != null ? support.sqlType() : (type.getName().startsWith("java.time.") ? "TIMESTAMP" : "TEXT");
    }

    private static Character toCharacter(final String value)
    {
        if (value == null || value.isEmpty())
        {
            throw new IllegalArgumentException("Cannot convert empty value to Character");
        }
        else if (value.length() != 1)
        {
            throw new IllegalArgumentException("Cannot convert multi-character value to Character");
        }
        return value.charAt(0);
    }

    private static Map<Class<?>, TypeSupport> createTypeSupport()
    {
        final Map<Class<?>, TypeSupport> support = new LinkedHashMap<>();
        add(support, Long.class, Long::valueOf, "BIGINT");
        add(support, long.class, Long::parseLong, "BIGINT");
        add(support, Integer.class, Integer::valueOf, "INT");
        add(support, int.class, Integer::parseInt, "INT");
        add(support, Short.class, Short::valueOf, "SMALLINT");
        add(support, short.class, Short::parseShort, "SMALLINT");
        add(support, Double.class, Double::valueOf, "DOUBLE");
        add(support, double.class, Double::parseDouble, "DOUBLE");
        add(support, Float.class, Float::valueOf, "FLOAT");
        add(support, float.class, Float::parseFloat, "FLOAT");
        add(support, Byte.class, Byte::valueOf, "TINYINT");
        add(support, byte.class, Byte::parseByte, "TINYINT");
        add(support, Character.class, SimpleDataObjectConverter::toCharacter, "CHAR(1)");
        add(support, char.class, SimpleDataObjectConverter::toCharacter, "CHAR(1)");
        add(support, BigInteger.class, BigInteger::new, "DECIMAL(38,0)");
        add(support, BigDecimal.class, BigDecimal::new, "DECIMAL(38,10)");
        add(support, Boolean.class, Boolean::valueOf, "BOOLEAN");
        add(support, boolean.class, Boolean::parseBoolean, "BOOLEAN");
        add(support, Instant.class, Instant::parse, "TIMESTAMP");
        add(support, LocalDate.class, LocalDate::parse, "TIMESTAMP");
        add(support, LocalDateTime.class, LocalDateTime::parse, "TIMESTAMP");
        add(support, LocalTime.class, LocalTime::parse, "TIMESTAMP");
        add(support, OffsetDateTime.class, OffsetDateTime::parse, "TIMESTAMP");
        add(support, OffsetTime.class, OffsetTime::parse, "TIMESTAMP");
        add(support, ZonedDateTime.class, ZonedDateTime::parse, "TIMESTAMP");
        add(support, String.class, s -> s, "TEXT");
        return Map.copyOf(support);
    }

    private static void add(
        final Map<Class<?>, TypeSupport> support,
        final Class<?> type,
        final Function<String, ?> parser,
        final String sqlType
    )
    {
        support.put(type, new TypeSupport(parser, sqlType));
    }

    private record TypeSupport(Function<String, ?> parser, String sqlType)
    {
    }
}
