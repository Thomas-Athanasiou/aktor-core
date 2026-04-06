package com.aktor.core.model;

public interface RepositoryFactoryLoader
{
    String kind();

    RepositoryFactory create(RepositoryProvider provider);
}
