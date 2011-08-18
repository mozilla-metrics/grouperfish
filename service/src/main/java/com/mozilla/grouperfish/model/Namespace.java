package com.mozilla.grouperfish.model;

import java.util.Map;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.batch.BatchScheduler;
import com.mozilla.grouperfish.bootstrap.Grouperfish;
import com.mozilla.grouperfish.rest.DocumentsResource;
import com.mozilla.grouperfish.rest.QueriesResource;
import com.mozilla.grouperfish.rest.ResultsResource;
import com.mozilla.grouperfish.rest.ConfigurationsResource.FilterConfigsResource;
import com.mozilla.grouperfish.rest.ConfigurationsResource.TransformConfigsResource;
import com.mozilla.grouperfish.services.Grid;


/**
 * Read-only information on a namespace. Cacheable.
 */
public class Namespace {

    public static Namespace get(String namespace) {
        return new Namespace(namespace);
    }

    private final int MAX_LENGTH = 512 * 1024;
    private final String name;
    private final Grid grid;

    private Namespace(String name) {
        this.name = name;
        grid = Grouperfish.services().grid();
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
        Assert.nonNull(resourceType);
        if (resourceType == ResultsResource.class) return results();
        if (resourceType == DocumentsResource.class) return documents();
        if (resourceType == QueriesResource.class) return queries();
        if (resourceType == TransformConfigsResource.class) return configurations(ConfigurationType.TRANSFOMS);
        if (resourceType == FilterConfigsResource.class) return configurations(ConfigurationType.FILTERS);
        Assert.unreachable("Unhandled resource type: %s", resourceType.getName());
        return null;
    }

    public BatchScheduler batchStarter() {
        return new BatchScheduler(this);
    }

    public Map<String, String> documents() {
        return grid.map("documents_" + name);
    }

    public Map<String, String> queries() {
        return grid.map("queries_" + name);
    }

    public Map<String, String> results() {
        return grid.map("results_" + name);
    }

    public Map<String, String> configurations(final ConfigurationType type) {
        Assert.nonNull(type);
        return grid.map("config_" + type.name().toLowerCase() + "_" + name);
    }

    public JsonValidator validator(final Class<?> resourceType) {
        return new JsonValidator();
    }
}
