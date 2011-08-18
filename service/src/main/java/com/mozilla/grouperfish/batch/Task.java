package com.mozilla.grouperfish.batch;

import java.io.Serializable;
import java.util.List;

import org.joda.time.Instant;

import com.google.common.collect.ImmutableList;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Namespace;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.model.TransformConfig;


/** Immutable task description. */
public class Task implements Serializable {

    // We do not want to serialize the namespace Object itself, but rather its name.
    private final String namespace;
    private final Query query;
    private final TransformConfig transform;
    private final Instant created;
    private final List<String> failures;

    public Task(final Namespace ns, final Query query, final TransformConfig transform) {
        Assert.nonNull(ns, query, transform);
        this.namespace = ns.toString();
        this.query = query;
        this.transform = transform;
        created = Instant.now();
        failures = ImmutableList.of();
    }

    private Task(final Task task, final String failure) {
        Assert.nonNull(task, failure);
        Assert.check(!failure.isEmpty());
        this.namespace = task.namespace;
        this.query = task.query;
        this.transform = task.transform;
        this.created = task.created();
        this.failures = new ImmutableList.Builder<String>().addAll(task.failures).add(failure).build();
    }

    public Namespace namespace() {
        return Namespace.get(namespace);
    }

    public Query query() {
        return query;
    }

    public String toString() {
        final String faildesc = (failures.size() == 0) ? "" : String.format(" (%s failed attempts)", failures.size());
        return String.format("[Task %s::%s::%s%s]", created(), transform, created(), faildesc);
    }

    public TransformConfig transform() {
        return transform;
    }

    public Task fail(final String failureMessage) {
        return new Task(this, failureMessage);
    }

    public List<String> failures() {
        return failures;
    }

    public Instant created() {
        return created;
    }

    private static final long serialVersionUID = 0;

}
