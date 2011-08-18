package com.mozilla.grouperfish.base;

import java.util.LinkedList;
import java.util.List;

public class Result<T> extends Box<T> {

    private final List<Exception> errors = new LinkedList<Exception>();

    public Result<T> error(String error) {
        errors.add(new Exception(error));
        return this;
    }

    public Result<T> error(Exception error) {
        errors.add(error);
        return this;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Iterable<Exception> errors() {
        return errors;
    }

}
