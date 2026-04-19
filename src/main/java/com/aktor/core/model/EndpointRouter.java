package com.aktor.web.http;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.Endpoint;
import com.aktor.core.model.EndpointResolver;
import com.aktor.core.model.FactoryContext;
import com.aktor.core.model.Configuration;
import com.aktor.core.model.Route;
import com.aktor.core.model.Router;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EndpointRouter
implements Router
{
    private record Registration(String route, Endpoint endpoint, com.aktor.core.model.EndpointMatcher matcher, long order)
    {
    }

    private final Map<String, Endpoint> endpoints = new LinkedHashMap<>();
    private final List<Registration> registrations = new ArrayList<>();
    private final FactoryContext context;
    private final com.aktor.core.model.EndpointFactory endpointFactory;
    private final EndpointResolver endpointResolver;
    private long sequence;

    public EndpointRouter()
    {
        this(null, null, null);
    }

    public EndpointRouter(final FactoryContext context, final com.aktor.core.model.EndpointFactory endpointFactory, final EndpointResolver endpointResolver)
    {
        this.context = context;
        this.endpointFactory = endpointFactory;
        this.endpointResolver = endpointResolver;
        if (context != null && endpointFactory != null)
        {
            loadConfiguredEndpoints(context.configuration());
        }
    }

    public EndpointRouter on(final String route, final Endpoint endpoint)
    {
        final String safeRoute = requireRoute(route);
        final Endpoint safeEndpoint = Objects.requireNonNull(endpoint);
        final com.aktor.core.model.EndpointMatcher matcher = com.aktor.core.model.EndpointMatcher.exact(safeRoute);
        endpoints.put(safeRoute, safeEndpoint);
        registrations.add(new Registration(safeRoute, safeEndpoint, matcher, sequence++));
        return this;
    }

    public EndpointRouter on(final String route, final Endpoint endpoint, final com.aktor.core.model.EndpointMatcher matcher)
    {
        final String safeRoute = requireRoute(route);
        final Endpoint safeEndpoint = Objects.requireNonNull(endpoint);
        final com.aktor.core.model.EndpointMatcher safeMatcher = Objects.requireNonNull(matcher, "matcher");
        endpoints.put(safeRoute, safeEndpoint);
        registrations.add(new Registration(safeRoute, safeEndpoint, safeMatcher, sequence++));
        return this;
    }

    public boolean has(final String route)
    {
        return endpoints.containsKey(requireRoute(route));
    }

    public Set<String> routes()
    {
        return Set.copyOf(endpoints.keySet());
    }

    @Override
    public Response route(final Route route, final Request request) throws ConversionException
    {
        final Route safeRoute = Objects.requireNonNull(route, "route");
        final Request safeRequest = Objects.requireNonNull(request, "request");
        final String method = safeRoute.method() == null || safeRoute.method().isBlank()
            ? safeRequest.method()
            : safeRoute.method();
        return route(new Request(
            method,
            safeRoute.path(),
            safeRequest.query(),
            safeRequest.headers(),
            safeRequest.body()
        ));
    }

    public Response route(final String route, final Request request) throws ConversionException
    {
        return route(new Route(request.method(), route), request);
    }

    public Response route(final Request request) throws ConversionException
    {
        final List<Registration> matches = matches(Objects.requireNonNull(request));
        trace("route", request.method() + " " + request.path() + " matches=" + matchNames(matches));
        if (matches.isEmpty())
        {
            if (endpointResolver == null)
            {
                throw new IllegalStateException("No endpoint matched request path: " + request.path());
            }
            return route(endpointResolver.resolve(request), request);
        }
        if (matches.size() == 1)
        {
            final Response response = matches.get(0).endpoint().convert(request);
            if (response == null)
            {
                throw new IllegalStateException("Matched endpoint produced no response for path: " + request.path());
            }
            return response;
        }

        final Endpoint[] before = new Endpoint[matches.size() - 1];
        for (int index = 0; index < before.length; index++)
        {
            before[index] = matches.get(index).endpoint();
        }
        final Endpoint main = matches.get(matches.size() - 1).endpoint();
        final Response response = new com.aktor.core.model.EndpointPipeline(before, main, new Endpoint[0]).convert(request);
        if (response == null)
        {
            throw new IllegalStateException("Matched endpoints produced no response for path: " + request.path());
        }
        return response;
    }

    private static String requireRoute(final String route)
    {
        return Objects.requireNonNull(route);
    }

    private Endpoint resolveEndpoint(final String route)
    {
        final Endpoint existing = endpoints.get(route);
        if (existing != null || endpointFactory == null)
        {
            return existing;
        }
        if (context == null)
        {
            throw new IllegalStateException("No factory context configured for endpoint creation");
        }
        final Endpoint created = endpointFactory.create(context, route);
        if (created != null)
        {
            endpoints.put(route, created);
        }
        return created;
    }

    private void loadConfiguredEndpoints(final Configuration configuration)
    {
        if (configuration == null)
        {
            return;
        }
        for (final String route : configuredEndpointNames(configuration))
        {
            final Configuration endpointConfig = configuration.getConfiguration(route);
            final Endpoint endpoint = endpointFactory.create(context, route);
            on(route, endpoint, com.aktor.core.model.EndpointMatcher.fromConfiguration(route, endpointConfig));
        }
    }

    private static List<String> configuredEndpointNames(final Configuration configuration)
    {
        final List<String> names = new ArrayList<>();
        for (final String key : configuration.keys())
        {
            if (key == null || key.isBlank())
            {
                continue;
            }
            final String route = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
            if (!route.isBlank() && !names.contains(route))
            {
                names.add(route);
            }
        }
        return names;
    }

    private List<Registration> matches(final Request request)
    {
        final List<Registration> result = new ArrayList<>();
        for (final Registration registration : registrations)
        {
            if (registration.matcher().matches(request))
            {
                result.add(registration);
            }
        }
        result.sort(Comparator
            .comparingInt((Registration registration) -> registration.matcher().specificity())
            .thenComparingLong(Registration::order));
        return result;
    }

    private static void trace(final String phase, final String message)
    {
        System.err.println("[EndpointRouter] " + phase + " " + message);
    }

    private static String matchNames(final List<Registration> matches)
    {
        if (matches == null || matches.isEmpty())
        {
            return "[]";
        }
        final StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < matches.size(); index++)
        {
            final Registration registration = matches.get(index);
            if (index > 0)
            {
                builder.append(", ");
            }
            builder.append(registration.route())
                .append("@")
                .append(registration.matcher().specificity());
        }
        return builder.append("]").toString();
    }
}
