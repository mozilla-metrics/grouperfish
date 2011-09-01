package com.mozilla.grouperfish.batch.handlers;

import com.mozilla.grouperfish.batch.scheduling.Helpers;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.FileSystem.Denied;
import com.mozilla.grouperfish.services.api.FileSystem.NotFound;


public class CleanupHandler implements TaskHandler {

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
            throw new Fail(task, "Could not cleanup task directory.", denied);
        }
        catch (final NotFound e) { // ok, ignore
        }

        return task;
    }
}
