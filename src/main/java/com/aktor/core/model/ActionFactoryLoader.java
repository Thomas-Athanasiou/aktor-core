package com.aktor.core.model;

public interface ActionFactoryLoader
extends Loader<ActionFactory<?, ?>>
{
    String kind();
}
