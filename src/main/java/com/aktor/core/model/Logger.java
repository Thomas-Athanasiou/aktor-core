package com.aktor.core.model;

public interface Logger
{
    void critical(String message);

    void error(String message);

    void warning(String message);

    void info(String message);

    void debug(String message);
}
