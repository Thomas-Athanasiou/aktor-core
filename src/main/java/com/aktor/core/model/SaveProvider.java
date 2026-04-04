package com.aktor.core.model;

import com.aktor.core.exception.SaveException;

@FunctionalInterface
public interface SaveProvider<Source, Item>
{
    void save(Source source, Item item) throws SaveException;
}
