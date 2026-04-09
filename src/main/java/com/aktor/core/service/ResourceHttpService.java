package com.aktor.core.service;

import com.aktor.core.model.HttpClient;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ResourceHttpService
implements ResourceService
{
    private final HttpClientFactory httpClientFactory;

    public ResourceHttpService()
    {
        this(HttpClient::new);
    }

    public ResourceHttpService(final HttpClientFactory httpClientFactory)
    {
        this.httpClientFactory = Objects.requireNonNull(httpClientFactory);
    }

    @Override
    public Response execute(final Request request) throws IOException
    {
        final Request safeRequest = Objects.requireNonNull(request);
        final HttpClient client = Objects.requireNonNull(httpClientFactory.create());
        client.clearHeaders();
        for (final Map.Entry<String, String> entry : safeRequest.headers().entrySet())
        {
            if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null)
            {
                client.addHeader(entry.getKey(), entry.getValue());
            }
        }
        if (safeRequest.timeoutMillis() != null && safeRequest.timeoutMillis() > 0)
        {
            client.setTimeout(safeRequest.timeoutMillis());
        }

        switch (safeRequest.method())
        {
            case GET -> client.get(safeRequest.uri());
            case POST -> client.post(safeRequest.uri(), safeRequest.body());
            case PUT -> client.put(safeRequest.uri(), safeRequest.body());
            case DELETE -> client.delete(safeRequest.uri(), safeRequest.body());
        }

        final LinkedHashMap<String, String> headers = new LinkedHashMap<>(client.getResponseHeaders());
        return new Response(client.getResponseStatus(), client.getResponseBody(), headers);
    }

    @FunctionalInterface
    public interface HttpClientFactory
    {
        HttpClient create();
    }
}
