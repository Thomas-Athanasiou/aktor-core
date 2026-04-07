package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;
import com.aktor.core.util.RecordTypeUtil;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ClassSqlSchemaParser
extends SqlStatementParserBase
implements Converter<Class<? extends Data<?>>, String>
{
    private final String keyField;

    private final FieldNormalizer fieldResolver;

    private ClassSqlSchemaParser(
        final String table,
        final String keyField,
        final String start,
        final String end,
        final FieldNormalizer fieldResolver
    )
    {
        super(table, start, end);
        this.keyField = Objects.requireNonNull(keyField);
        this.fieldResolver = Objects.requireNonNull(fieldResolver);
    }

    @Override
    public String convert(final Class<? extends Data<?>> input) throws ConversionException
    {
        if (!RecordTypeUtil.isRecordType(input))
        {
            throw new ConversionException("ClassSqlSchemaParser supports only record data types: " + input.getName());
        }

        final Map<String, String> columns = new LinkedHashMap<>();
        try
        {
            final RecordTypeUtil.ComponentDescriptor[] components = RecordTypeUtil.getRecordComponents(input);
            if (components.length > 0)
            {
                Arrays.stream(components).forEach(
                    component -> {
                        if (!isRelationComponentType(component.type()))
                        {
                            final String fieldName = fieldResolver.resolve(component.name());
                            if (fieldName != null && !fieldName.isBlank())
                            {
                                columns.put(
                                    fieldName,
                                    mapJavaTypeToSql(resolveSqlComponentType(component.type()))
                                );
                            }
                        }
                    }
                );
            }
            else
            {
                Arrays.stream(input.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .filter(field -> !field.isSynthetic())
                    .forEach(
                        field -> {
                            if (!isRelationComponentType(field.getType()))
                            {
                                final String fieldName = fieldResolver.resolve(field.getName());
                                if (fieldName != null && !fieldName.isBlank())
                                {
                                    columns.put(fieldName, mapJavaTypeToSql(resolveSqlComponentType(field.getType())));
                                }
                            }
                        }
                    );
            }
        }
        catch (final Exception exception)
        {
            throw new ConversionException(exception);
        }

        if (columns.isEmpty())
        {
            throw new ConversionException("No fields found for table schema in " + table);
        }

        final boolean relationType = RelationKeySpec.isRelationType(input);
        final String columnSql = columns.entrySet().stream()
            .map(
                entry -> {
                    final String name = entry.getKey();
                    final String value = entry.getValue();
                    final boolean isKey = !relationType && name.equals(keyField);
                    return joinParts(
                        new String[]
                        {
                            start + name + end,
                            isKey && value.equals("TEXT") ? "VARCHAR(255)" : value,
                            isKey ? "PRIMARY KEY" : ""
                        }
                    );
                }
            )
            .collect(Collectors.joining(","));
        final String relationPrimaryKeySql = relationType
            ? ", PRIMARY KEY (" + start + RelationKeySpec.MAIN + end + ", " + start + RelationKeySpec.FOREIGN + end + ")"
            : "";
        return "CREATE TABLE IF NOT EXISTS "
            + start
            + table
            + end
            + " ("
            + columnSql
            + relationPrimaryKeySql
            + ");";
    }

    private static String mapJavaTypeToSql(final Class<?> type)
    {
        return SimpleDataObjectConverter.sqlType(type);
    }

    private static boolean isRelationComponentType(final Class<?> type)
    {
        return type.isArray() && Data.class.isAssignableFrom(type.getComponentType());
    }

    private static Class<?> resolveSqlComponentType(final Class<?> type)
    {
        if (!Data.class.isAssignableFrom(type))
        {
            return type;
        }
        try
        {
            final Method keyMethod = type.getMethod("key");
            final Class<?> keyType = keyMethod.getReturnType();
            return Void.TYPE.equals(keyType) ? String.class : keyType;
        }
        catch (final NoSuchMethodException exception)
        {
            return String.class;
        }
    }

    private static String joinParts(final String[] parts)
    {
        return Arrays.stream(parts).filter(part -> part != null && !part.isBlank()).collect(Collectors.joining(" "));
    }

    public static Converter<Class<? extends Data<?>>, String> of(
        final String table,
        final String driver,
        final FieldNormalizer fieldResolver
    )
    {
        return of(table, driver, fieldResolver.resolve(LOGICAL_KEY_FIELD_NAME), fieldResolver);
    }

    public static Converter<Class<? extends Data<?>>, String> of(
        final String table,
        final String driver,
        final String keyField,
        final FieldNormalizer fieldResolver
    )
    {
        final SqlDialect dialect = SqlDialectResolver.of(driver);
        return new ClassSqlSchemaParser(
            table,
            keyField,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            fieldResolver
        );
    }
}
