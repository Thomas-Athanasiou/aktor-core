package com.aktor.core;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.RecordComponentFieldNameResolver;
import com.aktor.core.util.RecordTypeUtil;
import com.aktor.core.util.SimpleDataObjectConverter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassSqlSchemaParser
implements Converter<Class<? extends Data<?>>, String>
{
    private final String tableName;

    private final String keyFieldName;

    private final String start;

    private final String end;

    private final RecordComponentFieldNameResolver fieldNameResolver;

    public ClassSqlSchemaParser(
        final String tableName,
        final String keyFieldName,
        final String start,
        final String end
    )
    {
        this(tableName, keyFieldName, start, end, RecordComponentFieldNameResolver.DEFAULT);
    }

    public ClassSqlSchemaParser(
        final String tableName,
        final String keyFieldName,
        final String start,
        final String end,
        final RecordComponentFieldNameResolver fieldNameResolver
    )
    {
        super();
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
        this.tableName = Objects.requireNonNull(tableName);
        this.keyFieldName = Objects.requireNonNull(keyFieldName);
        this.fieldNameResolver = Objects.requireNonNull(fieldNameResolver);
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
            if (components != null && components.length > 0)
            {
                Arrays.stream(components).forEach(
                    component -> {
                        if (!isRelationComponentType(component.type()))
                        {
                            final String fieldName = fieldNameResolver.resolve(component.name());
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
                                final String fieldName = fieldNameResolver.resolve(field.getName());
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
            throw new ConversionException("No fields found for table schema in " + tableName);
        }

        final boolean relationType = RelationKeySpec.isRelationType(input);
        final String columnSql = columns.entrySet().stream()
            .map(
                entry -> {
                    final String name = entry.getKey();
                    final String value = entry.getValue();
                    final boolean isKey = !relationType && name.equals(keyFieldName);
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
            ? ", PRIMARY KEY (" + start + RelationKeySpec.MAIN_FIELD + end + ", " + start + RelationKeySpec.FOREIGN_FIELD + end + ")"
            : "";
        return "CREATE TABLE IF NOT EXISTS "
            + start
            + tableName
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
            return keyType == null || Void.TYPE.equals(keyType) ? String.class : keyType;
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
}
