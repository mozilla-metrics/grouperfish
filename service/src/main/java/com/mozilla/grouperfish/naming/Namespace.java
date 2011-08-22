package com.mozilla.grouperfish.naming;



/**
 * Scopes resource access to a namespace.
 */
public class Namespace {

    private final String namespace;

    public Namespace(final String namespace) {
        this.namespace = namespace;
    }

    public String raw() {
        return namespace;
    }

    public String String() {
        return raw();
    }
}
