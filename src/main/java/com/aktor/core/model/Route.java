package com.aktor.web.http;

import java.util.Objects;

public record Route(String method, String path)
{
    public Route
    {
        Objects.requireNonNull(method);
        Objects.requireNonNull(path);
    }
}
