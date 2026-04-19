package com.aktor.core.model;

import com.aktor.core.exception.ConversionException;
import com.aktor.web.http.EndpointRouter;
import com.aktor.web.http.Request;
import com.aktor.web.http.Response;

import java.util.Objects;

public final class RouterDefault
implements Router
{
    private final EndpointRouter router;

    public RouterDefault(final EndpointProvider provider)
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
        return router.route(request);
    }
}
