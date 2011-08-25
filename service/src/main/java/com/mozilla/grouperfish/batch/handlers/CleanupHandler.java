package com.mozilla.grouperfish.batch.handlers;

import com.mozilla.grouperfish.batch.Helpers;
import com.mozilla.grouperfish.batch.Fail;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.FileSystem.Denied;
import com.mozilla.grouperfish.services.FileSystem.NotFound;


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
