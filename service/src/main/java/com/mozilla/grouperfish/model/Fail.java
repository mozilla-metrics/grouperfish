package com.mozilla.grouperfish.model;


/**
 * Describes how a task failed. Generated by handlers.
 * This information should be made available somehow under the run resource.
 */
public abstract class Fail extends Exception {

    private static final long serialVersionUID = 0;

    private final Task task;

    public static Fail hard(final Task task, final String message, final Throwable maybeCause) {
        if (maybeCause == null) return new HardFail(task, message);
        return new HardFail(task, message, maybeCause);
    }

    public static Fail soft(final Task task, final String message, final Throwable maybeCause) {
        if (maybeCause == null) return new SoftFail(task, message);
        return new SoftFail(task, message, maybeCause);
    }

    public Fail(final Task task, final String message) {
        super(String.format("Task %s failed. %s", task, message));
        this.task = task;
    }

    public Fail(final Task task, final String message, final Throwable cause) {
        super(message, cause);
        this.task = task;
    }

    public Task task() {
        return task;
    }

    /**
     * Handlers can throw a hard failure if they are fairly certain that
     * retrying will not help.
     */
    public static final class HardFail extends Fail {
        HardFail(final Task task, final String message, final Throwable cause) {
            super(task, message, cause);
        }

        HardFail(final Task task, final String message) {
            super(task, message);
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Handlers can throw a soft failure if they think that
     * retrying might help, e.g. if they were interrupted during execution.
     */
    public static final class SoftFail extends Fail {
        SoftFail(final Task task, final String message, final Throwable cause) {
            super(task, message, cause);
        }

        SoftFail(final Task task, final String message) {
            super(task, message);
        }

        private static final long serialVersionUID = 1L;
    }
}
