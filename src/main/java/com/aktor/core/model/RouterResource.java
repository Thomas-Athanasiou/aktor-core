package com.aktor.core.model;

import com.aktor.core.exception.ConversionException;
import com.aktor.web.http.EndpointRouter;
import com.aktor.web.http.Request;
import com.aktor.web.http.Response;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RouterResource
implements Router
{
    private final EndpointRouter router;

    public RouterResource(final EndpointProvider provider)
    {
        Objects.requireNonNull(provider);
        this.router = new EndpointRouter(provider, (context, name) -> provider.endpoint(name), request -> "*");
    }

    @Override
    public Response route(final Route route, final Request request) throws ConversionException
    {
        return router.route(route, request);
    }

    @Override
    public Response route(final Request request) throws ConversionException
    {
        final Map<String, String> params = parseQuery(request.query());
        final String target = params.getOrDefault("uri", "/");
        final String query = buildQueryWithoutUri(params);
        final Request translated = new Request(
            request.method(),
            target,
            query,
            request.headers(),
            request.body()
        );
        return router.route(translated);
    }

    private static Map<String, String> parseQuery(final String query)
    {
        final Map<String, String> result = new LinkedHashMap<>();
        if (query == null || query.isBlank())
        {
            return result;
        }
        final String[] pairs = query.split("&");
        for (final String pair : pairs)
        {
            if (pair == null || pair.isBlank())
            {
                continue;
            }
            final int index = pair.indexOf('=');
            final String key = index < 0 ? pair : pair.substring(0, index);
            final String value = index < 0 ? "" : pair.substring(index + 1);
            final String decodedKey = decode(key);
            if (decodedKey == null || decodedKey.isBlank())
            {
                continue;
            }
            result.put(decodedKey, decode(value));
        }
        return result;
    }

    private static String buildQueryWithoutUri(final Map<String, String> params)
    {
        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, String> entry : params.entrySet())
        {
            final String key = entry.getKey();
            if (key == null || key.isBlank() || "uri".equalsIgnoreCase(key))
            {
                continue;
            }
            if (!builder.isEmpty())
            {
                builder.append("&");
            }
            builder.append(encode(key)).append("=").append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private static String decode(final String value)
    {
        if (value == null)
        {
            return "";
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String encode(final String value)
    {
        if (value == null)
        {
            return "";
        }
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
