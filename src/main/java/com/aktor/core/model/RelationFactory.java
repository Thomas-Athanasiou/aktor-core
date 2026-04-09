package com.aktor.core.model;

import com.aktor.core.data.Relation;

@FunctionalInterface
public interface RelationFactory<MainKey, ForeignKey>
{
    Relation<MainKey, ForeignKey> create(MainKey mainKey, ForeignKey foreignKey);
}
