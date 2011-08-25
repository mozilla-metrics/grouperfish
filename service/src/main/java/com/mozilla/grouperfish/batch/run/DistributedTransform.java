package com.mozilla.grouperfish.batch.run;

import com.mozilla.grouperfish.batch.Helpers;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.FileSystem.FsError;


/** Transform that relies on a distributed fs for processing. */
public class DistributedTransform extends ExecutableTransform {

    public DistributedTransform(final String name, final FileSystem dfs) {
        super(name, dfs);
    }

    @Override
    protected String workDirectoryUri(final Task task) throws FsError {
        return dataFs().uri(Helpers.taskDirectory(task));
    }

    @Override
    public boolean requiresDfs() {
        return true;
    }

}
