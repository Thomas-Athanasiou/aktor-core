package com.aktor.core.value;

import com.aktor.core.ConditionType;

import java.util.Objects;

public record Filter(String field, String value, ConditionType conditionType)
{
    public Filter
    {
        Objects.requireNonNull(field);
        Objects.requireNonNull(conditionType);
    }
}
