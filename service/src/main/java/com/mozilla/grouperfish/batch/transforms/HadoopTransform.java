package com.mozilla.grouperfish.batch.transforms;

import com.mozilla.grouperfish.batch.scheduling.Helpers;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.FileSystem.FsError;


/** Transform that relies on a distributed fs for processing. */
public class HadoopTransform extends ExecutableTransform {

    public HadoopTransform(final String name, final FileSystem dfs) {
        super(name, dfs);
    }

    @Override
    protected String taskDirectoryUri(final Task task) throws FsError {
        return dataFs().uri(Helpers.taskDirectory(task));
    }

}
