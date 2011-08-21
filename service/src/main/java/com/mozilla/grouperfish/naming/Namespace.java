package com.mozilla.grouperfish.naming;

import com.mozilla.grouperfish.json.JsonValidator;
import com.mozilla.grouperfish.model.Access;


/**
 * Scopes resource access to a namespace.
 */
public class Namespace {

    private final int MAX_LENGTH = 512 * 1024;
    private final String namespace;

    public Namespace(final String namespace) {
        this.namespace = namespace;
    }

    public String raw() {
        return namespace;
    }

    public int maxLength(Class<?> resourceType, Access access) {
        return MAX_LENGTH;
    }

    public boolean allows(Class<?> resourceType, Access access) {
        return true;
    }

    public JsonValidator validator(final Class<?> resourceType) {
        return new JsonValidator();
    }
}
