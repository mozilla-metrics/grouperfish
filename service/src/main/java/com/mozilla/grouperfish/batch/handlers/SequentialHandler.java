package com.mozilla.grouperfish.batch.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;


/**
 * Composite handler.
 *
 * Applies all sub-handlers synchronously, in order.
 * Can be helpful to simplify things for development/testing
 * (compared to pipelining).
 */
public class SequentialHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(SequentialHandler.class);

    private final TaskHandler[] handlers;

    public SequentialHandler(final TaskHandler... handlers) {
        this.handlers = handlers;
    }

    @Override
    public Task handle(Task task) throws Fail {
        for (final TaskHandler handler : handlers) {
            log.debug("Task {}: starting handler {}", task, handler.getClass().getSimpleName());
            task = handler.handle(task);
        }
        return task;
    }
}
