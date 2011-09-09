package com.mozilla.grouperfish.batch.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.mozilla.grouperfish.batch.handlers.TaskHandler;
import com.mozilla.grouperfish.batch.transforms.TransformProvider;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Grid;
import com.mozilla.grouperfish.services.api.IndexProvider;


/**
 * Braindead fully synchronous "batch" service.
 *
 * It has no queue, no multithreading.
 * It just executes everything right away, while you wait for results.
 *
 * Can be useful in testing/development.
 */
public class SynchronousBatchService extends AbstractBatchService {

    private static final Logger log = LoggerFactory.getLogger(SynchronousBatchService.class);

    private final TaskHandler handler;

    @Inject
    public SynchronousBatchService(
            final Grid grid,
            final IndexProvider indexes,
            final FileSystem fs,
            final TransformProvider transforms) {
        super(indexes);
        handler = Helpers.sequentialHandler(grid, fs, indexes, transforms);

        log.info("Instantiated service: {}", getClass().getSimpleName());
    }

    @Override
    public void schedule(final Task task) {
        try {
            handler.handle(task);
        }
        catch (Fail e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() { }

    @Override
    public void stop() { }

}
