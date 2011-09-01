package com.mozilla.grouperfish.rest.jaxrs;


import com.mozilla.grouperfish.naming.Scope;
import com.mozilla.grouperfish.services.api.Grid;

public class ResourceBase {

    private final Grid grid;

    public ResourceBase(final Grid grid) {
        this.grid = grid;
    }

    protected Scope scope(final String namespace) {
        return new Scope(namespace, grid);
    }


}
