package com.aktor.core.model;

public interface Loader<Type>
{
    String kind();

    Type load(Environment environment);
}
