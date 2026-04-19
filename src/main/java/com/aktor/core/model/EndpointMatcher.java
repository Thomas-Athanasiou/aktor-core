package com.aktor.core.model;

import com.aktor.web.http.Request;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record EndpointMatcher(
    Set<String> methods,
    Pattern pathPattern,
    int specificity
)
{
    public EndpointMatcher
    {
        methods = methods == null ? Set.of() : Set.copyOf(methods);
        pathPattern = Objects.requireNonNull(pathPattern, "pathPattern");
    }

    public boolean matches(final Request request)
    {
        final Request safeRequest = Objects.requireNonNull(request, "request");
        if (!methods.isEmpty())
        {
            final String method = safeRequest.method();
            if (method == null || method.isBlank())
            {
                return false;
            }
            final String safeMethod = method.trim().toUpperCase(Locale.ROOT);
            if (!methods.contains(safeMethod))
            {
                return false;
            }
        }
        final String path = safeRequest.path();
        return path != null && pathPattern.matcher(path).matches();
    }

    public static EndpointMatcher exact(final String path)
    {
        final String safePath = requireText(path, "path");
        return new EndpointMatcher(Set.of(), Pattern.compile(Pattern.quote(safePath)), scoreExact(safePath));
    }

    public static EndpointMatcher fromConfiguration(final String name, final Configuration configuration)
    {
        final String safeName = requireText(name, "name");
        final Configuration safeConfiguration = Objects.requireNonNull(configuration, "configuration");
        final Set<String> methods = methods(safeConfiguration);
        final String pathPattern = firstNonBlank(
            safeConfiguration.getString("pathPattern"),
            safeConfiguration.getString("path"),
            safeName
        );
        final int specificity = specificity(pathPattern, methods.isEmpty());
        return new EndpointMatcher(methods, compile(pathPattern), specificity);
    }

    private static Set<String> methods(final Configuration configuration)
    {
        final Set<String> result = new LinkedHashSet<>();
        final String single = configuration.getString("method");
        if (single != null && !single.isBlank())
        {
            result.add(single.trim().toUpperCase(Locale.ROOT));
        }
        final String multiple = configuration.getString("methods");
        if (multiple != null && !multiple.isBlank())
        {
            for (final String token : multiple.split(","))
            {
                if (token != null && !token.isBlank())
                {
                    result.add(token.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        return result;
    }

    private static Pattern compile(final String pathPattern)
    {
        final String safePattern = requireText(pathPattern, "pathPattern");
        final StringBuilder regex = new StringBuilder();
        for (int index = 0; index < safePattern.length(); index++)
        {
            final char current = safePattern.charAt(index);
            if (current == '*')
            {
                final boolean doubleStar = index + 1 < safePattern.length() && safePattern.charAt(index + 1) == '*';
                regex.append(doubleStar ? ".*" : "[^/]*");
                if (doubleStar)
                {
                    index++;
                }
                continue;
            }
            if (".[]{}()+-^$|\\".indexOf(current) >= 0)
            {
                regex.append('\\');
            }
            regex.append(current);
        }
        return Pattern.compile("^" + regex + "$");
    }

    private static int specificity(final String pathPattern, final boolean noMethods)
    {
        final String safePattern = requireText(pathPattern, "pathPattern");
        int score = 0;
        for (int index = 0; index < safePattern.length(); index++)
        {
            final char current = safePattern.charAt(index);
            if (current != '*' && current != '?')
            {
                score++;
            }
        }
        if (!noMethods)
        {
            score += 100;
        }
        return score;
    }

    private static int scoreExact(final String path)
    {
        return 1_000 + requireText(path, "path").length();
    }

    private static String firstNonBlank(final String first, final String second, final String third)
    {
        if (first != null && !first.isBlank())
        {
            return first.trim();
        }
        if (second != null && !second.isBlank())
        {
            return second.trim();
        }
        if (third != null && !third.isBlank())
        {
            return third.trim();
        }
        return null;
    }

    private static String requireText(final String value, final String name)
    {
        final String safe = Objects.requireNonNull(value, name);
        if (safe.isBlank())
        {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return safe.trim();
    }
}
