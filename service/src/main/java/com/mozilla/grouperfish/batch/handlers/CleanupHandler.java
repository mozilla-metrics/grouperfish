package com.mozilla.grouperfish.batch.handlers;

import com.mozilla.grouperfish.base.Result;
import com.mozilla.grouperfish.batch.RetryException;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.services.FileSystem;


public class CleanupHandler implements TaskHandler {

    private final FileSystem fs;

    public CleanupHandler(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public Task handle(final Task task) throws RetryException {
        final Result<String> result = fs.removeRecursively(Helpers.taskDirectory(task));
        for (final Exception e : result.error()) throw new RetryException("Failed to clean out directory.", e);
        return task;
    }
}
