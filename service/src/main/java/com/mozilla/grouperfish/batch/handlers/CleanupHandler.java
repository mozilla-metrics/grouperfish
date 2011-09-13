package com.mozilla.grouperfish.batch.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.batch.scheduling.Helpers;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.FileSystem.Denied;
import com.mozilla.grouperfish.services.api.FileSystem.NotFound;


public class CleanupHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(CleanupHandler.class);


    private final FileSystem fs;

    public CleanupHandler(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public Task handle(final Task task) throws Fail {
        try {
            fs.removeRecursively(Helpers.taskDirectory(task));
        }
        catch (final Denied denied) {
            throw Fail.hard(task, "Could not cleanup task directory.", denied);
        }
        catch (final NotFound e) {
            // ok, ignore
            log.debug("Missing task directory during cleanup, this can indicate problems. Task: %s", task);
        }

        return task;
    }
}
