package com.mozilla.grouperfish.base;


public class Result<T> extends Box<T> {

    private final Box<Exception> error = new Box<Exception>();

    public Result<T> error(final String message) {
        error.put(new Exception(message));
        return this;
    }

    public Result<T> error(final Exception e) {
        error.put(e);
        return this;
    }

    public boolean hasErrors() {
        return !error.empty();
    }

    public Box<Exception> error() {
        return error;
    }

}
