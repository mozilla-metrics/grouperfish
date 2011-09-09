package com.mozilla.grouperfish.batch.scheduling;

import java.util.Map;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.batch.api.BatchService;
import com.mozilla.grouperfish.model.Type;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.model.TransformConfig;
import com.mozilla.grouperfish.naming.Scope;
import com.mozilla.grouperfish.services.api.Index;
import com.mozilla.grouperfish.services.api.IndexProvider;

abstract class AbstractBatchService implements BatchService {

    private final IndexProvider indexes;

    public AbstractBatchService(final IndexProvider indexes) {
        this.indexes = indexes;
    }

    /** Run the configured transform over the query results. */
    public void schedule(final Scope ns, final Query query, final TransformConfig transform) {
        Assert.nonNull(query, transform);
        final Index index = indexes.index(ns.name(Type.DOCUMENT));
        for (final Query concreteQuery : index.resolve(query)) {
            schedule(new Task(ns, concreteQuery, transform));
        }
    }

    /** Run all configured transforms over the query results. */
    public void schedule(final Scope ns, final Query query) {
        final Map<String, String> transforms = ns.map(Type.CONFIGURATION_TRANSFORM);
        for (final Map.Entry<String, String> item : transforms.entrySet()) {
            schedule(ns, query, new TransformConfig(item.getKey(), item.getValue()));
        }
    }

    /** Run all transforms configurations of this namespace over the results of all queries. */
    public void schedule(final Scope ns) {
        final Map<String, String> queries = ns.queries();
        final Map<String, String> transforms = ns.map(Type.CONFIGURATION_TRANSFORM);
        for (final Map.Entry<String, String> queryEntry : queries.entrySet()) {
            final Query query = new Query(queryEntry.getKey(), queryEntry.getValue());
            for (final Map.Entry<String, String> item : transforms.entrySet()) {
                schedule(ns, query, new TransformConfig(item.getKey(), item.getValue()));
            }
        }
    }

}
