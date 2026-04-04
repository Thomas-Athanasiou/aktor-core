package com.aktor.core.service;

import com.aktor.core.model.HttpClient;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class HttpRequestService
{
    private final HttpClientFactory httpClientFactory;

    public HttpRequestService()
    {
        this(HttpClient::new);
    }

    public HttpRequestService(final HttpClientFactory httpClientFactory)
    {
        this.httpClientFactory = Objects.requireNonNull(httpClientFactory);
    }

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

        final String method = safeRequest.method().trim().toUpperCase();
        switch (method)
        {
            case "GET" -> client.get(safeRequest.uri());
            case "POST" -> client.post(safeRequest.uri(), safeRequest.body());
            case "PUT" -> client.put(safeRequest.uri(), safeRequest.body());
            case "DELETE" -> client.delete(safeRequest.uri(), safeRequest.body());
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + safeRequest.method());
        }

        final LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        for (final String name : safeRequest.responseHeaderNames())
        {
            final String value = client.getResponseHeader(name);
            if (value != null)
            {
                headers.put(name, value);
            }
        }
        return new Response(client.getResponseStatus(), client.getResponseBody(), headers);
    }

    @FunctionalInterface
    public interface HttpClientFactory
    {
        HttpClient create();
    }

    public record Request(
        String method,
        String uri,
        String body,
        Integer timeoutMillis,
        Map<String, String> headers,
        String[] responseHeaderNames
    )
    {
        public Request
        {
            method = Objects.requireNonNullElse(method, "GET");
            uri = Objects.requireNonNull(uri);
            headers = new LinkedHashMap<>(headers == null ? Map.of() : headers);
            responseHeaderNames = responseHeaderNames == null ? new String[0] : responseHeaderNames.clone();
        }
    }

    public record Response(
        int status,
        String body,
        Map<String, String> headers
    )
    {
        public Response
        {
            headers = new LinkedHashMap<>(headers == null ? Map.of() : headers);
        }
    }
}
