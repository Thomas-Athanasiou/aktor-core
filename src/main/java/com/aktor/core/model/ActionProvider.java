package com.aktor.core.model;

import com.aktor.core.Action;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ActionProvider<Route, Target>
implements Router<Route, Target>
{
    private final Map<Route, Action<? super Target>> actions = new LinkedHashMap<>();
    private final ActionFactory<Route, Target> actionFactory;
    private final ActionResolver<Route, Target> actionResolver;

    public ActionProvider()
    {
        this(null, null);
    }

    public ActionProvider(
        final ActionFactory<Route, Target> actionFactory,
        final ActionResolver<Route, Target> actionResolver
    )
    {
        this.actionFactory = actionFactory;
        this.actionResolver = actionResolver;
    }

    public ActionProvider<Route, Target> on(final Route route, final Action<? super Target> action)
    {
        actions.put(requireRoute(route), Objects.requireNonNull(action));
        return this;
    }

    public boolean has(final Route route)
    {
        return actions.containsKey(requireRoute(route));
    }

    public Set<Route> routes()
    {
        return Set.copyOf(actions.keySet());
    }

    @Override
    public void route(final Route route, final Target target)
    {
        final Route safeRoute = requireRoute(route);
        final Action<? super Target> action = resolveAction(safeRoute);
        if (action == null)
        {
            throw new IllegalArgumentException("No action registered for route: " + safeRoute);
        }
        action.execute(target);
    }

    @Override
    public void route(final Target target)
    {
        if (actionResolver == null)
        {
            throw new IllegalStateException("No action resolver configured for target routing");
        }
        route(actionResolver.resolve(target), target);
    }

    private static <Route> Route requireRoute(final Route route)
    {
        return Objects.requireNonNull(route);
    }

    private Action<? super Target> resolveAction(final Route route)
    {
        final Action<? super Target> existing = actions.get(route);
        if (existing != null || actionFactory == null)
        {
            return existing;
        }
        final Action<? super Target> created = actionFactory.create(route);
        if (created != null)
        {
            actions.put(route, created);
        }
        return created;
    }
}
