package com.mozilla.grouperfish.batch.scheduling;

import java.util.concurrent.BlockingQueue;

import com.google.inject.Inject;
import com.mozilla.grouperfish.batch.transforms.TransformProvider;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Grid;
import com.mozilla.grouperfish.services.api.Index;

/**
 * Run everything using one queue and a single worker.
 * Mostly useful to test the worker.
 */
public class SingleQueueBatchService extends AbstractBatchService {

    private final Worker worker;
    private final BlockingQueue<Task> inQueue;
    private final BlockingQueue<Task> failQueue;

    @Override
    public void schedule(Task task) {
        inQueue.add(task);
    }

    @Inject
    public SingleQueueBatchService(
            final Grid grid,
            final Index index,
            final FileSystem fs,
            final TransformProvider transforms) {

        super(index);
        inQueue = grid.queue("grouperfish_in");
        failQueue = grid.queue("grouperfish_fail");

        worker = new Worker(failQueue, inQueue, null, Helpers.sequentialHandler(grid, fs, index, transforms));

    }

    public void start() {
        worker.start();
    }

    public void stop() {
        worker.cancel();
    }


}
