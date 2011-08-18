package com.mozilla.grouperfish.batch;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Namespace;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.model.TransformConfig;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import java.util.concurrent.BlockingQueue;


/**
 * Implements the batch system component as documented at:
 * http://grouperfish.readthedocs.org/en/latest/batch_system.html
 */
public class BatchScheduler {

    private final Namespace ns;

    public BatchScheduler(final Namespace ns) {
        this.ns = ns;
    }

    /** Run the configured transform over the query results. */
    public void schedule(final Query query, final TransformConfig transform) {
        Assert.nonNull(query, transform);
        BlockingQueue<BatchTask> queue = Hazelcast.getQueue("run_tasks");
        queue.add(new BatchTask(query, transform));

    }

    /** Run all configured transforms over the query results. */
    public void schedule(final Query query) {
        final Config cfg = Hazelcast.getConfig();
        cfg.getMapConfig("documents_" + ns.toString());
    }

    /** Run all transforms configurations of this namespace over the results of all queries. */
    public void schedule() {

    }

}
