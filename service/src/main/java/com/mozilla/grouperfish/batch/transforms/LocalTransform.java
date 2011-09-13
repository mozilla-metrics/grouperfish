package com.mozilla.grouperfish.batch.transforms;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.batch.scheduling.Helpers;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.FileSystem.FsError;


/**
 * A transform can be implemented as a local executable that does not
 * know about hadoop or how to talk to HDFS, and instead uses a
 * (temporary) local work directory.
 *
 * Such transforms are made available through the LocalTransform
 * wrapper which will copy inputs from HDFS to the local file system,
 * and results back to HDFS.
 *
 * The actual executable will receive a local directory (as an absolute
 * path) instead of an HDFS uri.
 */
public class LocalTransform extends ExecutableTransform {

    private final FileSystem localFs;
    private final boolean needsToCopy;

    /**
     * A local transform in a distributed environment:
     * Task input data is copied from the dfs to the local fs before
     * running, and results are copied back afterwards.
     *
     * @param name The transform executable. It should take the location of the input data
     *             as its single argument.
     * @param dfs The distributed filesystem used by grouperfish (e.g. HDFS).
     * @param localFs The local filesystem where working directories for local processes can be created.
     */
    public LocalTransform(
            final String name,
            final FileSystem dfs,
            final FileSystem localFs) {
        super(name, dfs);
        Assert.nonNull(localFs);
        this.localFs = localFs;
        this.needsToCopy = !dfs.equals(localFs);
    }

    @Override
    protected String taskDirectoryUri(final Task task) throws FsError {
        return localFs.uri(Helpers.taskDirectory(task)).substring("file://".length());
    }

    @Override
    public TransformResult run(Task task) throws Fail, InterruptedException {
        if (needsToCopy) {
            try {
                Helpers.copy(Helpers.inputFilename(task), dataFs(), localFs);
                Helpers.copy(Helpers.parametersFilename(task), dataFs(), localFs);
            }
            catch (final Exception e) {
                throw Fail.hard(task, "Could not copy data to local fs.", e);
            }
        }

        final TransformResult result = super.run(task);

        if (needsToCopy) {
            try {
                Helpers.copy(Helpers.resultsFilename(task), localFs, dataFs());
            }
            catch (final Exception e) {
                throw Fail.hard(task, "Could not copy results back to distributed fs.", e);
            }
        }

        return result;
    }

}
