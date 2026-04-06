package com.aktor.core.model;

import com.aktor.core.util.SimpleDataObjectConverter;

@FunctionalInterface
public interface FieldNameNormalizer
{
    FieldNameNormalizer DEFAULT = SimpleDataObjectConverter::camelToSnake;

    String resolve(String componentName);
}
