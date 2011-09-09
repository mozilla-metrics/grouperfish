package com.mozilla.grouperfish.naming;

import java.util.EnumMap;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Type;


/**
 * Scopes resource access to a namespace.
 */
public class Namespace {

    protected final String namespace;

    public Namespace(final String namespace) {
        if (namespace.indexOf('.') != -1) {
            IllegalStateException e =new IllegalStateException("Illegal namespace: " + namespace);
            e.printStackTrace();
            throw e;
        }
        this.namespace = namespace;
    }

    @SuppressWarnings("serial")
    private static final EnumMap<Type, String> prefixes = new EnumMap<Type, String>(Type.class) {{
        for (Type t : Type.values()) {
            switch (t) {
                case DOCUMENT: put(t, "documents_"); break;
                case QUERY: put(t, "queries_"); break;
                case CONFIGURATION_FILTER: put(t, "configurations_filters_"); break;
                case CONFIGURATION_TRANSFORM: put(t, "configurations_transforms_"); break;
                case RESULT: put(t, "results_"); break;
                default: Assert.unreachable();
            }
        }
    }};

    public final String name(final Type type) {
        return prefixes.get(type) + namespace;
    }

    public String raw() {
        return namespace;
    }

    public String toString() {
        return String.format("[Namespace %s]", raw());
    }
}
