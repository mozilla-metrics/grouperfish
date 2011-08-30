package com.mozilla.grouperfish.batch.transforms;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.FileSystem.FsError;


/**
 * Transform based on launching a subprocess, with an executable
 * locally available on the grouperfish node.
 * Right now, this is the only transform we have.
 */
abstract class ExecutableTransform implements Transform {

    public static final String TRANSFORMS_DIRECTORY = "./transforms";

    private final String name;
    private final FileSystem dataFs;
    private final File exe;
    private final File transformDir;

    public String name() {
        return name;
    }

    public FileSystem dataFs() {
        return dataFs;
    }

    protected File executable() {
        return exe;
    }

    /**
     * Return the URL of the working directory to use, and make sure
     * that it actually exists and contains the required input data.
     */
    protected abstract String workDirectoryUri(final Task task) throws FsError;

    public ExecutableTransform(final String name, final FileSystem dataFs) {
        Assert.nonNull(name, dataFs);
        Assert.check(!name.isEmpty());
        this.name = name;
        this.dataFs = dataFs;

        final String transformPath = String.format("%s/%s", TRANSFORMS_DIRECTORY, name);
        final String exePath = String.format("%s/%s", transformPath, name);

        transformDir = new File(transformPath);
        if (!transformDir.exists() || !transformDir.isDirectory())
            Assert.unreachable(
                    "Cannot find transform directory '%s'. (working directory: '%s')",
                    transformPath, System.getProperty("user.dir"));

        exe = new File(exePath);

        if (!exe.exists())
            Assert.unreachable(
                    "Cannot find executable '%s'. (working directory: '%s')",
                    exePath, System.getProperty("user.dir"));

        if (!exe.isFile() || !exe.canExecute())
            Assert.unreachable("Cannot execute '%s'.", exe.getAbsolutePath());
    }


    @Override
    public TransformResult run(final Task task) throws Fail, InterruptedException {
        final String workDirectoryUri;
        try {
            workDirectoryUri = workDirectoryUri(task);
        }
        catch (final FsError e) {
            throw new Fail(task, "Could not access task work directory!");
        }

        final ProcessBuilder pb = new ProcessBuilder(name(), workDirectoryUri);
        pb.directory(transformDir);
        try {
            final Process process = pb.start();
            final boolean success = process.waitFor() == 0;
            return new TransformResult() {
                @Override
                public InputStream stderr() { return process.getErrorStream(); }
                @Override
                public boolean success() { return success; }
            };
        }
        catch (final IOException e) {
            throw new Fail(task, "IO problem during transform execution.", e);
        }
    }

}
