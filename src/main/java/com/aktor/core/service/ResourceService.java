package com.aktor.core.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public interface ResourceService
{
    Response execute(Request request) throws IOException;

    enum Method
    {
        GET,
        POST,
        PUT,
        DELETE;

        public static Method parse(final String value)
        {
            if (value == null || value.isBlank())
            {
                return GET;
            }
            return Method.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        }
    }

    record Request(Method method, String uri, String body, Map<String, String> headers, Options options)
    {
        public static final Options EMPTY_OPTIONS = new Options(null);

        public Request
        {
            method = Objects.requireNonNullElse(method, Method.GET);
            uri = Objects.requireNonNull(uri);
            headers = new LinkedHashMap<>(headers == null ? Map.of() : headers);
            options = Objects.requireNonNullElse(options, EMPTY_OPTIONS);
        }

        public Integer timeoutMillis()
        {
            return options.timeoutMillis();
        }
    }

    record Response(int status, String body, Map<String, String> headers)
    {
        public Response
        {
            headers = new LinkedHashMap<>(headers == null ? Map.of() : headers);
        }
    }

    record Options(Integer timeoutMillis)
    {
    }
}
