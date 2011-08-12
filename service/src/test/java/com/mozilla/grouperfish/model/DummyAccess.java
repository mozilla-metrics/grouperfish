package com.mozilla.grouperfish.model;


public class DummyAccess implements Access {

    private final String origin;
    private final Type type;

    public DummyAccess(Type type, String origin) {
        this.origin = origin;
        this.type = type;
    }

    @Override
    public String origin() {
        return origin;
    }

    @Override
    public Type type() {
        return type;
    }

}
