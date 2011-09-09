package com.mozilla.grouperfish.naming;

import java.util.Map;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.base.json.JsonValidator;
import com.mozilla.grouperfish.model.Access;
import com.mozilla.grouperfish.model.Type;
import com.mozilla.grouperfish.rest.jaxrs.DocumentsResource;
import com.mozilla.grouperfish.rest.jaxrs.QueriesResource;
import com.mozilla.grouperfish.rest.jaxrs.ResultsResource;
import com.mozilla.grouperfish.rest.jaxrs.ConfigurationsResource.FilterConfigsResource;
import com.mozilla.grouperfish.rest.jaxrs.ConfigurationsResource.TransformConfigsResource;
import com.mozilla.grouperfish.services.api.Grid;


/**
 * Helps to consistently associate resource access to a namespace.
 * Gatekeeperfor each access, allows to implement permissions
 * (in {@link #allows(Class, Access)}).
 */
public class Scope extends Namespace {

    private final Grid grid;
    private final int maxDocumentLength = 512 * 1024 * 1024;

    public Scope(final String namespace, final Grid grid) {
        super(namespace);
        this.grid = grid;
    }

    public Scope(final Namespace ns, final Grid grid) {
        super(ns.raw());
        this.grid = grid;
    }

    public Map<String, String> documents() {
        return grid.map(name(Type.DOCUMENT));
    }

    public Map<String, String> queries() {
        return grid.map(name(Type.QUERY));
    }

    public Map<String, String> results() {
        return grid.map(name(Type.RESULT));
    }

    public Map<String, String> map(final Type type) {
        Assert.nonNull(type);
        return grid.map(name(type));
    }

    public Map<String, String> resourceMap(final Class<?> resourceType) {
        Assert.nonNull(resourceType);
        if (resourceType == ResultsResource.class) return results();
        if (resourceType == DocumentsResource.class) return documents();
        if (resourceType == QueriesResource.class) return queries();
        if (resourceType == TransformConfigsResource.class) return map(Type.CONFIGURATION_TRANSFORM);
        if (resourceType == FilterConfigsResource.class) return map(Type.CONFIGURATION_FILTER);
        Assert.unreachable("Unhandled resource type: %s", resourceType.getName());
        return null;
    }

    public int maxLength(final Class<?> resourceType, final Access access) {
        return maxDocumentLength;
    }

    public boolean allows(final Class<?> resourceType, final Access access) {
        return true;
    }

    public JsonValidator validator(final Class<?> resourceType) {
        return new JsonValidator();
    }
}