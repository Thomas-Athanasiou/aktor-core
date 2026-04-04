package com.aktor.core;

public interface Action<Target>
{
    void execute(final Target values);
}
