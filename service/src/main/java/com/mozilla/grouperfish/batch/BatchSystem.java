package com.mozilla.grouperfish.batch;

import com.google.common.collect.ImmutableList;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.batch.handlers.CleanupHandler;
import com.mozilla.grouperfish.batch.handlers.FetchHandler;
import com.mozilla.grouperfish.batch.handlers.PutHandler;
import com.mozilla.grouperfish.batch.handlers.RunHandler;
import com.mozilla.grouperfish.bootstrap.Grouperfish;
import com.mozilla.grouperfish.model.ConfigurationType;
import com.mozilla.grouperfish.model.Namespace;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.model.TransformConfig;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.Grid;
import com.mozilla.grouperfish.services.Index;
import com.mozilla.grouperfish.services.Services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;


/**
 * Implements the batch system component as documented at:
 * http://grouperfish.readthedocs.org/en/latest/batch_system.html
 *
 * Queues:
 * -------
 * |...prepare...|
 *               |...run...|
 *                         |...put...|
 *                                   |...tag...|
 *                                             |...cleanup...|
 *
 * Each queue is processed concurrently by several workers, running on each cluster node.
 */
public class BatchSystem {

    private final Namespace ns;
    private final Grid grid;
    private final Index index;

    private final List<Worker> workers;
    private final BlockingQueue<Task> prepareQueue;

    public BatchSystem(final Namespace ns) {
        this.ns = ns;
        final Services services = Grouperfish.services();
        grid = services.grid();
        index = services.index();
        final FileSystem fs = services.fs();

        final BlockingQueue<Task> prepQ = grid.queue("grouperfish_prepare");
        final BlockingQueue<Task> runQ = grid.queue("grouperfish_run");
        final BlockingQueue<Task> putQ = grid.queue("grouperfish_putresult");
        final BlockingQueue<Task> cleanupQ = grid.queue("grouperfish_cleanup");

        final BlockingQueue<Task> failQ = grid.queue("grouperfish_fail");

        // Here we could in theory create e.g. several run workers instead of just one for this host...
        workers = new ImmutableList.Builder<Worker>()
            .add(new Worker(failQ, prepQ, runQ, new FetchHandler(fs, index)))
            .add(new Worker(failQ,        runQ, putQ, new RunHandler(fs)))
            .add(new Worker(failQ,              putQ, cleanupQ, new PutHandler(grid, fs)))
            .add(new Worker(failQ,                    cleanupQ, null, new CleanupHandler(fs)))
            .build();

        prepareQueue = prepQ;
    }

    public void startWorkers() {
        for (final Worker worker : workers) worker.start();
    }

    public void killWorkers() {
        for (final Worker worker : workers) worker.cancel();
    }

    /** Run the configured transform over the query results. */
    public void schedule(final Query query, final TransformConfig transform) {
        Assert.nonNull(query, transform);
        final BlockingQueue<Task> queue = prepareQueue;
        for (final Query concreteQuery : index.resolve(ns, query)) {
            queue.add(new Task(ns, concreteQuery, transform));
        }
    }

    /** Run all configured transforms over the query results. */
    public void schedule(final Query query) {
        final Map<String, String> transforms = ns.configurations(ConfigurationType.TRANSFOMS);
        for (final Map.Entry<String, String> item : transforms.entrySet()) {
            schedule(query, new TransformConfig(item.getKey(), item.getValue()));
        }
    }

    /** Run all transforms configurations of this namespace over the results of all queries. */
    public void schedule() {
        final Map<String, String> queries = ns.queries();
        final Map<String, String> transforms = ns.configurations(ConfigurationType.TRANSFOMS);
        for (final Map.Entry<String, String> queryEntry : queries.entrySet()) {
            final Query query = new Query(queryEntry.getKey(), queryEntry.getValue());
            for (final Map.Entry<String, String> item : transforms.entrySet()) {
                schedule(query, new TransformConfig(item.getKey(), item.getValue()));
            }
        }
    }

}
