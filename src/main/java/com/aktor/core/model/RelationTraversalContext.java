package com.aktor.core.model;

import com.aktor.core.exception.ModelException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class RelationTraversalContext
{
    final Deque<Frame> stack = new ArrayDeque<>();

    public RelationTraversalContext()
    {
        super();
    }

    Scope enterRead(final Class<?> type, final Object key, final String field, final RelationCyclePolicy policy)
    throws ModelException
    {
        return enter(Operation.READ, type, key, field, policy);
    }

    Scope enterSave(final Class<?> type, final Object key, final String field, final RelationCyclePolicy policy)
    throws ModelException
    {
        return enter(Operation.SAVE, type, key, field, policy);
    }

    private Scope enter(
        final Operation operation,
        final Class<?> type,
        final Object key,
        final String field,
        final RelationCyclePolicy policy
    )
    throws ModelException
    {
        final Frame next = new Frame(
            Objects.requireNonNull(type),
            Objects.requireNonNull(key),
            field == null || field.isBlank() ? "unknown" : field
        );
        final Frame existing = findFrame(next.type(), next.key());
        if (existing != null)
        {
            if (policy == RelationCyclePolicy.LINK_EXISTING)
            {
                return new Scope(this, existing, true);
            }
            throw new ModelException(buildMessage(operation.label(), next));
        }
        stack.push(next);
        return new Scope(this, next, false);
    }

    private String buildMessage(final String operation, final Frame next)
    {
        final List<Frame> path = new ArrayList<>(stack);
        final int loopStart = findFrameIndex(next.type(), next.key());
        final StringBuilder builder = new StringBuilder("Circular relation traversal detected during ").append(operation).append(": ");
        if (loopStart < 0)
        {
            builder.append(next.display());
            return builder.toString();
        }
        for (int index = path.size() - 1; index >= loopStart; index--)
        {
            builder.append(path.get(index).display()).append(" -> ");
        }
        builder.append(next.display());
        return builder.toString();
    }

    private enum Operation
    {
        READ("get"),
        SAVE("save");

        private final String label;

        Operation(final String label)
        {
            this.label = label;
        }

        String label()
        {
            return label;
        }
    }

    private Frame findFrame(final Class<?> type, final Object key)
    {
        for (final Frame frame : stack)
        {
            if (frame.type().equals(type) && frame.key().equals(key))
            {
                return frame;
            }
        }
        return null;
    }

    private int findFrameIndex(final Class<?> type, final Object key)
    {
        final List<Frame> frames = new ArrayList<>(stack);
        for (int index = 0; index < frames.size(); index++)
        {
            final Frame frame = frames.get(index);
            if (frame.type().equals(type) && frame.key().equals(key))
            {
                return index;
            }
        }
        return -1;
    }

    record Frame(Class<?> type, Object key, String field)
    {
        String display()
        {
            return type.getSimpleName() + "#" + key + "." + field;
        }
    }

    static final class Scope
    implements AutoCloseable
    {
        private final RelationTraversalContext context;
        private final Frame frame;
        private final boolean linked;
        private boolean closed = false;

        Scope(final RelationTraversalContext context, final Frame frame, final boolean linked)
        {
            this.context = context;
            this.frame = frame;
            this.linked = linked;
        }

        boolean linked()
        {
            return linked;
        }

        @Override
        public void close()
        {
            if (closed)
            {
                return;
            }
            closed = true;
            if (linked || context == null)
            {
                return;
            }
            if (!context.stack.isEmpty() && context.stack.peek().equals(frame))
            {
                context.stack.pop();
            }
        }
    }
}
