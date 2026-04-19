package com.aktor.core.model;

import com.aktor.core.exception.ConversionException;import com.aktor.web.http.Request;import com.aktor.web.http.Response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RouterComposite
implements Router
{
    private final Map<String, Router> routes = new LinkedHashMap<>();

    public RouterComposite on(final String pathPrefix, final Router router)
    {
        routes.put(requirePath(pathPrefix), Objects.requireNonNull(router));
        return this;
    }

    @Override
    public Response route(final Route route, final Request request) throws ConversionException
    {
        final Router router = routes.get(requirePath(route.path()));
        if (router == null)
        {
            throw new IllegalArgumentException("No router registered for: " + route.path());
        }
        return router.route(route, request);
    }

    @Override
    public Response route(final Request request) throws ConversionException
    {
        final String path = requirePath(request.path());
        Router best = null;
        int bestLength = -1;
        for (final Map.Entry<String, Router> entry : routes.entrySet())
        {
            final String prefix = entry.getKey();
            if (path.startsWith(prefix) && prefix.length() > bestLength)
            {
                best = entry.getValue();
                bestLength = prefix.length();
            }
        }
        if (best == null)
        {
            throw new IllegalArgumentException("No router registered for path: " + path);
        }
        return best.route(request);
    }

    private static String requirePath(final String path)
    {
        return Objects.requireNonNull(path);
    }
}
