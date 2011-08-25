package com.mozilla.grouperfish.batch.run;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.batch.Fail;
import com.mozilla.grouperfish.batch.Helpers;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.FileSystem.FsError;


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
     * @param name The transform executable.
     * @param dfs The distributed filesystem used by grouperfish (HDFS).
     * @param localFs The local filesystem where working directories for tasks can be created.
     */
    public LocalTransform(
            final String name,
            final FileSystem dfs,
            final FileSystem localFs) {
        super(name, dfs);
        Assert.nonNull(localFs);
        Assert.check(dfs != localFs);
        this.localFs = localFs;
        this.needsToCopy = true;
    }

    /**
     * A local transform in a local (stand-alone) environment.
     * Since everything uses the local fs anyway, no copying
     * is needed / possible.
     */
    public LocalTransform(final String name, final FileSystem localFs) {
        super(name, localFs);
        Assert.nonNull(localFs);
        this.localFs = localFs;
        this.needsToCopy = false;
    }

    @Override
    protected String workDirectoryUri(final Task task) throws FsError {
        return localFs.uri(Helpers.taskDirectory(task));
    }

    @Override
    public TransformResult run(Task task) throws Fail, InterruptedException {
        if (needsToCopy) {
            try {
                Helpers.copy(Helpers.inputFilename(task), dataFs(), localFs);
                Helpers.copy(Helpers.parametersFilename(task), dataFs(), localFs);
            }
            catch (final Exception e) {
                throw new Fail.SoftFail(task, "Could not copy data to local fs.", e);
            }
        }

        final TransformResult result = super.run(task);

        if (needsToCopy) {
            try {
                Helpers.copy(Helpers.resultsFilename(task), localFs, dataFs());
            }
            catch (final Exception e) {
                throw new Fail.SoftFail(task, "Could not copy results back to distributed fs.", e);
            }
        }

        return result;
    }

    @Override
    public boolean requiresDfs() {
        return false;
    }

}
