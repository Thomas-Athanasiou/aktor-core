package com.aktor.core.model;

import com.aktor.core.Model;
import com.aktor.core.value.Filter;

import java.util.Map;

interface FilterCondition
extends Model
{
    boolean isEntityMatch(final Map<String, String> fieldMap, final Filter filter);
}
