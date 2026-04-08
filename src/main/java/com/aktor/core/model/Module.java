package com.aktor.core.model;

public interface Module
{
    String kind();

    void setup(Environment environment);
}
