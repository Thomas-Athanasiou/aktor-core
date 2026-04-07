package com.aktor.core.model;

import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Collections;

public record RowMappingContext(Map<String, String> row, Class<?> itemType, String keyField)
{
    public RowMappingContext
    {
        row = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(row)));
        Objects.requireNonNull(itemType);
        keyField = Objects.requireNonNull(keyField);
    }
}
