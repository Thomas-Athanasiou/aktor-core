package com.aktor.core.model;

// TODO MAYBE SOME Module<> super-interface??
public interface RepositoryFactoryLoader
{
    String kind();

    RepositoryFactory create(RepositoryProvider provider);
}
