package com.aktor.core.model;

public interface Environment
{
    <Type> void put(Type service);

    <Type> Type get(Class<Type> type);

    <Type> Type require(Class<Type> type);

    <Type> void registerLoader(Class<Type> type, Loader<? extends Type> loader);

    <Type> Type load(Class<Type> type, String kind);
}
