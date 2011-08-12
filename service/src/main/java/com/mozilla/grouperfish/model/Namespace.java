package com.mozilla.grouperfish.model;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.Hazelcast;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.service.ConfigurationsResource.FilterConfigsResource;
import com.mozilla.grouperfish.service.ConfigurationsResource.TransformConfigsResource;
import com.mozilla.grouperfish.service.DocumentsResource;
import com.mozilla.grouperfish.service.QueriesResource;
import com.mozilla.grouperfish.service.ResultsResource;


/**
 * Read-only information on a namespace. Cacheable.
 */
public class Namespace {

    public static Namespace get(String namespace) {
        return new Namespace(namespace);
    }

    private final int MAX_LENGTH = 512 * 1024;
    private final String name;

    static final Set<String> CONFIG_TYPES = ImmutableSet.of("filters", "transforms");

    private Namespace(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public int maxLength(Class<?> resourceType, Access access) {
        return MAX_LENGTH;
    }

    public boolean allows(Class<?> resourceType, Access access) {
        return true;
    }

    public Map<String, String> resourceMap(Class<?> resourceType) {
        if (resourceType == ResultsResource.class) return results();
        if (resourceType == DocumentsResource.class) return documents();
        if (resourceType == QueriesResource.class) return queries();
        if (resourceType == TransformConfigsResource.class) return configurations("transforms");
        if (resourceType == FilterConfigsResource.class) return configurations("filters");
        Assert.unreachable("Unhandled resource type: %s", resourceType.getName());
        return null;
    }

    public Map<String, String> documents() {
        return Hazelcast.getMap("documents_" + name);
    }

    public Map<String, String> queries() {
        return Hazelcast.getMap("queries_" + name);
    }

    public Map<String, String> results() {
        return Hazelcast.getMap("results_" + name);
    }

    public Map<String, String> configurations(final String type) {
        if (!CONFIG_TYPES.contains(type)) Assert.unreachable("No such configuration type: %s", type);
        return Hazelcast.getMap("config_" + type + "_" + name);
    }

    public JsonValidator validator(final Class<?> resourceType) {
        return new JsonValidator();
    }
}
