package com.mozilla.grouperfish.batch.scheduling;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.mozilla.grouperfish.batch.handlers.CleanupHandler;
import com.mozilla.grouperfish.batch.handlers.FetchHandler;
import com.mozilla.grouperfish.batch.handlers.PutHandler;
import com.mozilla.grouperfish.batch.handlers.RunHandler;
import com.mozilla.grouperfish.batch.transforms.TransformProvider;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Grid;
import com.mozilla.grouperfish.services.api.Index;


/**
 * Implements the batch system as multiple distributed queues.
 * Assuming that the levels of the queue have different
 * resource usage (network, IO, CPU, RAM) characteristics,
 * this aims to always interleave handlers from every queue.
 *
 * Queues:
 * -------
 * |...prepare...|
 *               |...run...|
 *                         |...put...|
 *                                   |...cleanup...|
 *
 * Each queue is processed concurrently by multiple workers,
 * running on each cluster node.
 *
 * A more advanced system could move specific handlers to
 * specific grouperfish nodes for optimum provisioning, or
 * vary the global number of handlers for any queue depending
 * on the queue size.
 */
public class PipeliningBatchService extends AbstractBatchService {

    private static final Logger log = LoggerFactory.getLogger(PipeliningBatchService.class);

    private final List<Worker> workers;
    private final BlockingQueue<Task> prepareQueue;

    @Inject
    public PipeliningBatchService(
            final Grid grid,
            final Index index,
            final FileSystem fs,
            final TransformProvider transforms) {
        super(index);

        final BlockingQueue<Task> prepQ = grid.queue("grouperfish_prepare");
        final BlockingQueue<Task> runQ = grid.queue("grouperfish_run");
        final BlockingQueue<Task> putQ = grid.queue("grouperfish_putresult");
        final BlockingQueue<Task> cleanupQ = grid.queue("grouperfish_cleanup");

        final BlockingQueue<Task> failQ = grid.queue("grouperfish_fail");

        // Here we could in theory create e.g. several run workers instead of just one for this host...
        workers = new ImmutableList.Builder<Worker>()
            .add(new Worker(failQ, prepQ, runQ, new FetchHandler(fs, index)))
            .add(new Worker(failQ,        runQ, putQ, new RunHandler(fs, transforms)))
            .add(new Worker(failQ,              putQ, cleanupQ, new PutHandler(grid, fs)))
            .add(new Worker(failQ,                    cleanupQ, null, new CleanupHandler(fs)))
            .build();

        prepareQueue = prepQ;
        log.info("Instantiated service: {}", getClass().getSimpleName());
    }

    public void start() {
        for (final Worker worker : workers) worker.start();
    }

    public void stop() {
        for (final Worker worker : workers) worker.cancel();
    }

    @Override
    public void schedule(Task task) {
        // TODO Auto-generated method stub
        final BlockingQueue<Task> queue = prepareQueue;
        queue.add(task);
    }

}
