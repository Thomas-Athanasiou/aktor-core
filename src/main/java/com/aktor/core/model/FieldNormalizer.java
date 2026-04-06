package com.aktor.core.model;

import com.aktor.core.util.SimpleDataObjectConverter;

@FunctionalInterface
public interface FieldNormalizer
{
    FieldNormalizer DEFAULT = SimpleDataObjectConverter::camelToSnake;

    String resolve(String componentName);
}
