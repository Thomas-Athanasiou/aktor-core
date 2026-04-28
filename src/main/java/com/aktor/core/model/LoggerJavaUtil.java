package com.aktor.core.model;

import java.util.Objects;
import java.util.logging.Level;

public final class LoggerJavaUtil
implements Logger
{
    public static final String DEFAULT_NAME = "com.aktor";

    private final java.util.logging.Logger logger;

    public LoggerJavaUtil()
    {
        this(DEFAULT_NAME);
    }

    public LoggerJavaUtil(final Class<?> type)
    {
        this(type == null ? DEFAULT_NAME : type.getName());
    }

    public LoggerJavaUtil(final String name)
    {
        this.logger = java.util.logging.Logger.getLogger(
            name == null || name.isBlank() ? DEFAULT_NAME : name.trim()
        );
    }

    public LoggerJavaUtil(final java.util.logging.Logger logger)
    {
        this.logger = Objects.requireNonNull(logger);
    }

    @Override
    public void critical(final String message)
    {
        logger.severe(safe(message));
    }

    @Override
    public void error(final String message)
    {
        logger.severe(safe(message));
    }

    @Override
    public void warning(final String message)
    {
        logger.warning(safe(message));
    }

    @Override
    public void info(final String message)
    {
        logger.info(safe(message));
    }

    @Override
    public void debug(final String message)
    {
        logger.log(Level.FINE, safe(message));
    }

    private static String safe(final String message)
    {
        return message == null ? "" : message;
    }
}
