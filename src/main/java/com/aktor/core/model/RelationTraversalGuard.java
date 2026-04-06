package com.aktor.core.model;

import com.aktor.core.exception.ModelException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

final class RelationTraversalGuard
{
    static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private RelationTraversalGuard()
    {
    }

    static Scope enterRead(final Class<?> itemType, final Object key, final String field)
    throws ModelException
    {
        return enter("get", itemType, key, field);
    }

    static Scope enterSave(final Class<?> itemType, final Object key, final String field)
    throws ModelException
    {
        return enter("save", itemType, key, field);
    }

    private static Scope enter(final String operation, final Class<?> itemType, final Object key, final String field)
    throws ModelException
    {
        final Frame next = new Frame(
            Objects.requireNonNull(itemType),
            Objects.requireNonNull(key),
            field == null || field.isBlank() ? "unknown" : field
        );
        final Deque<Frame> stack = STACK.get();
        if (stack.contains(next))
        {
            throw new ModelException(buildMessage(operation, stack, next));
        }
        stack.push(next);
        return new Scope();
    }

    private static String buildMessage(final String operation, final Deque<Frame> stack, final Frame next)
    {
        final List<Frame> path = new ArrayList<>(stack);
        final int loopStart = path.indexOf(next);
        final StringBuilder builder = new StringBuilder("Circular relation traversal detected during ").append(operation).append(": ");
        for (int index = path.size() - 1; index >= loopStart; index--)
        {
            builder.append(path.get(index).display()).append(" -> ");
        }
        builder.append(next.display());
        return builder.toString();
    }

    record Frame(Class<?> itemType, Object key, String field)
    {
        String display()
        {
            return itemType.getSimpleName() + "#" + key + "." + field;
        }
    }

    static final class Scope
    implements AutoCloseable
    {
        @Override
        public void close()
        {
            final Deque<Frame> stack = STACK.get();
            if (!stack.isEmpty())
            {
                stack.pop();
            }
            if (stack.isEmpty())
            {
                STACK.remove();
            }
        }
    }
}
