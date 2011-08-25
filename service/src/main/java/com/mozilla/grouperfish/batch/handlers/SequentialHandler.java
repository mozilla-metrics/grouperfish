package com.mozilla.grouperfish.batch.handlers;

import com.mozilla.grouperfish.batch.Fail;
import com.mozilla.grouperfish.batch.Task;


/**
 * Composite handler.
 *
 * Applies all sub-handlers synchronously, in order.
 * Can be helpful to simplify things for development/testing
 * (compared to pipelining).
 */
public class SequentialHandler implements TaskHandler {

    private final TaskHandler[] handlers;

    public SequentialHandler(final TaskHandler... handlers) {
        this.handlers = handlers;
    }

    @Override
    public Task handle(Task task) throws Fail {
        for (final TaskHandler handler : handlers)
            task = handler.handle(task);
        return task;
    }
}
