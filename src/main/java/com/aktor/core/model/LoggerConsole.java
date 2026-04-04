package com.aktor.core.model;

@SuppressWarnings("Singleton")
public final class LoggerConsole
implements Logger
{
    public static final LoggerConsole INSTANCE = new LoggerConsole();

    private LoggerConsole()
    {
        super();
    }

    @Override
    public void critical(final String message)
    {
        System.err.println("[CRITICAL] " + message);
    }

    @Override
    public void error(final String message)
    {
        System.err.println("[ERROR] " + message);
    }

    @Override
    public void warning(final String message)
    {
        System.out.println("[WARN] " + message);
    }

    @Override
    public void info(final String message)
    {
        System.out.println("[INFO] " + message);
    }

    @Override
    public void debug(final String message)
    {
        System.out.println("[DEBUG] " + message);
    }
}

