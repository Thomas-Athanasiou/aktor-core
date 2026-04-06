package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.data.Relation;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.util.CompositeKeyUtil;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class FieldNormalizer
implements FieldNameNormalizer
{
    private static final String LOGICAL_KEY_FIELD_NAME = "key";

    private static final Pattern SAFE_FIELD = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private static final Map<CacheKey, FieldNormalizer> TYPE_CACHE = new ConcurrentHashMap<>();

    public record ResolvedValue(String field, String rawValue)
    {

    }

    private final String[] fields;

    private final Map<String, String> aliases;

    private final Map<String, String> resolvedCache = new ConcurrentHashMap<>();

    private FieldNormalizer(final String[] fields, final Map<String, String> aliases)
    {
        this.fields = Arrays.copyOf(Objects.requireNonNull(fields), fields.length);
        this.aliases = Map.copyOf(Objects.requireNonNull(aliases));
    }

    public static FieldNormalizer mapped(final Class<? extends Data<?>> type)
    {
        return mapped(type, Map.of());
    }

    public static FieldNormalizer mapped(final Class<? extends Data<?>> type, final Map<String, String> mappedFields)
    {
        final CacheKey cacheKey = new CacheKey(
            Objects.requireNonNull(type),
            Map.copyOf(Objects.requireNonNull(mappedFields))
        );
        return TYPE_CACHE.computeIfAbsent(cacheKey, FieldNormalizer::buildForType);
    }

    public String resolveRawKey(
        final RowMappingContext context,
        final int keyComponentIndex,
        final String keyComponentName,
        final String keyComponentSnakeName
    ) throws ConversionException
    {
        final String keyField = resolve(LOGICAL_KEY_FIELD_NAME);
        String key = context.row().get(keyField);
        if ((key == null || key.isEmpty()) && keyComponentIndex >= 0)
        {
            key = resolveValue(context, keyComponentIndex, keyComponentName, keyComponentSnakeName).rawValue();
        }
        if ((key == null || key.isEmpty()) && Relation.class.isAssignableFrom(context.itemType()))
        {
            final String mainKey = resolveValueByCandidates(context.row(), "main_key").rawValue();
            final String foreignKey = resolveValueByCandidates(context.row(), "foreign_key").rawValue();
            if (mainKey != null && !mainKey.isEmpty() && foreignKey != null && !foreignKey.isEmpty())
            {
                key = CompositeKeyUtil.encode(new String[]{mainKey, foreignKey});
            }
        }

        if (key == null || key.isEmpty())
        {
            throw new ConversionException("Missing key value for parent object: " + keyField);
        }

        return key;
    }

    public ResolvedValue resolveValue(
        final RowMappingContext context,
        final int index,
        final String componentName,
        final String componentSnakeName
    )
    {
        final String configured = index < fields.length ? fields[index] : null;
        return resolveValueByCandidates(context.row(), configured, componentName, componentSnakeName);
    }

    public String[] fields()
    {
        return Arrays.copyOf(fields, fields.length);
    }

    @Override
    public String resolve(final String requestedField)
    {
        final String safeRequestedField = Objects.requireNonNull(requestedField, "field").trim();
        if (!SAFE_FIELD.matcher(safeRequestedField).matches())
        {
            throw new IllegalArgumentException("Illegal field name: " + safeRequestedField);
        }
        return resolvedCache.computeIfAbsent(safeRequestedField, this::resolveCached);
    }

    private String resolveCached(final String safeRequestedField)
    {
        final String exactMatch = aliases.get(safeRequestedField);
        if (exactMatch != null)
        {
            return exactMatch;
        }

        final String normalized = SimpleDataObjectConverter.camelToSnake(safeRequestedField);
        final String normalizedMatch = aliases.get(normalized);
        if (normalizedMatch != null)
        {
            return normalizedMatch;
        }

        throw new IllegalArgumentException("Unknown field: " + safeRequestedField);
    }

    private static FieldNormalizer buildForType(final CacheKey cacheKey)
    {
        try
        {
            final RecordTypePlan plan = RecordTypePlan.of(cacheKey.type());
            final String actualKeyFieldName = firstNonBlank(
                cacheKey.mappedFields().get(LOGICAL_KEY_FIELD_NAME),
                cacheKey.mappedFields().get(SimpleDataObjectConverter.camelToSnake(LOGICAL_KEY_FIELD_NAME)),
                LOGICAL_KEY_FIELD_NAME
            );
            final String[] orderedFields = new String[plan.size()];
            final Map<String, String> aliases = new LinkedHashMap<>();
            for (int index = 0; index < plan.size(); index++)
            {
                final String componentName = plan.componentName(index);
                final String componentSnakeName = plan.componentSnakeName(index);
                final String configuredColumn = firstNonBlank(
                    cacheKey.mappedFields().get(componentName),
                    cacheKey.mappedFields().get(componentSnakeName)
                );
                final String actualColumn = index == plan.keyComponentIndex()
                    ? actualKeyFieldName
                    : configuredColumn != null && !configuredColumn.isBlank()
                        ? configuredColumn
                        : componentSnakeName;
                orderedFields[index] = actualColumn;

                putAlias(aliases, componentName, actualColumn);
                putAlias(aliases, componentSnakeName, actualColumn);
                putAlias(aliases, actualColumn, actualColumn);
                if (configuredColumn != null && !configuredColumn.isBlank())
                {
                    putAlias(aliases, configuredColumn, actualColumn);
                }
            }
            for (final Map.Entry<String, String> entry : cacheKey.mappedFields().entrySet())
            {
                final String alias = entry.getKey();
                final String column = entry.getValue();
                if (alias != null && !alias.isBlank() && column != null && !column.isBlank())
                {
                    putAlias(aliases, alias, column);
                }
            }
            return new FieldNormalizer(orderedFields, aliases);
        }
        catch (final ConversionException exception)
        {
            throw new IllegalArgumentException("Cannot build field resolver for type: " + cacheKey.type().getName(), exception);
        }
    }

    private static ResolvedValue resolveValueByCandidates(final Map<String, String> map, final String candidate)
    {
        return resolveValueByCandidates(map, candidate, null, null);
    }

    private static ResolvedValue resolveValueByCandidates(
        final Map<String, String> map,
        final String firstCandidate,
        final String secondCandidate,
        final String thirdCandidate
    )
    {
        ResolvedValue resolvedValue = resolveCandidate(map, firstCandidate);
        if (resolvedValue != null)
        {
            return resolvedValue;
        }
        resolvedValue = resolveCandidate(map, secondCandidate);
        if (resolvedValue != null)
        {
            return resolvedValue;
        }
        resolvedValue = resolveCandidate(map, thirdCandidate);
        return Objects.requireNonNullElseGet(
            resolvedValue,
            () -> new ResolvedValue(firstNonBlank(firstCandidate, secondCandidate, thirdCandidate), null)
        );
    }

    private static ResolvedValue resolveCandidate(final Map<String, String> map, final String candidate)
    {
        if (candidate == null || candidate.isBlank() || !map.containsKey(candidate))
        {
            return null;
        }
        return new ResolvedValue(candidate, map.get(candidate));
    }

    private static String firstNonBlank(final String... candidates)
    {
        for (final String candidate : candidates)
        {
            if (candidate != null && !candidate.isBlank())
            {
                return candidate;
            }
        }
        return null;
    }

    private static void putAlias(final Map<String, String> aliases, final String alias, final String column)
    {
        final String previous = aliases.putIfAbsent(alias, column);
        if (previous != null && !previous.equals(column))
        {
            throw new IllegalArgumentException(
                "Field alias '" + alias + "' is mapped to both '" + previous + "' and '" + column + "'"
            );
        }
    }

    record CacheKey(Class<? extends Data<?>> type, Map<String, String> mappedFields)
    {

    }
}
