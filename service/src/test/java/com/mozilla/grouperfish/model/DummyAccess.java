package com.mozilla.grouperfish.model;


public class DummyAccess implements Access {

    private final String origin;
    private final Operation type;

    public DummyAccess(Operation type, String origin) {
        this.origin = origin;
        this.type = type;
    }

    @Override
    public String origin() {
        return origin;
    }

    @Override
    public Operation type() {
        return type;
    }

}
