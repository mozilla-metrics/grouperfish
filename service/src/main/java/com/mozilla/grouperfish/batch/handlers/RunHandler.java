package com.mozilla.grouperfish.batch.handlers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.base.StreamTool;
import com.mozilla.grouperfish.batch.scheduling.Helpers;
import com.mozilla.grouperfish.batch.transforms.Transform;
import com.mozilla.grouperfish.batch.transforms.Transform.TransformResult;
import com.mozilla.grouperfish.batch.transforms.TransformProvider;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.model.TransformConfig;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.FileSystem.Denied;
import com.mozilla.grouperfish.services.api.FileSystem.NotFound;


/** Perform the actual running of the transform. */
public class RunHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(RunHandler.class);

    private final FileSystem fs;
    private final TransformProvider transforms;

    public RunHandler(final FileSystem fs, final TransformProvider transforms) {
        this.fs = fs;
        this.transforms = transforms;
    }

    @Override
    public Task handle(final Task task) throws Fail {
        final String inputDirectory;
        try {
            inputDirectory = fs.uri(Helpers.taskDirectory(task));
        }
        catch (final NotFound e) {
            throw Fail.hard(task, "Task input not found...", e);
        }

        try {
            fs.makeDirectory(Helpers.outputDirectory(task));
        } catch (final Denied e) {
            throw Fail.hard(task, "Cannot create output directory.", e);
        }

        final TransformConfig config = task.transform();
        final Transform transform = transforms.get(config.transform());
        Assert.nonNull(transform);
        log.info(String.format("Launching transform '%s' with input directory '%s'", transform, inputDirectory));

        try {
            final TransformResult result = transform.run(task);
            if (result.success()) {
                log.info("Transform {} for task {} was run successfully.", transform, task);
            }
            else {
                final String message = String.format("Failed to run transform: %s (task %s)", transform, task);
                log.warn(message);
                log.warn("STDERR: {}", StreamTool.consume(result.stderr(), StreamTool.UTF8));
                throw Fail.hard(task, message, null);
            }
        }
        catch (final InterruptedException e) {
            throw Fail.soft(task, "Interrupted during run.", e);
        }
        catch (final IOException e) {
            throw Fail.hard(task, "Received IO error reading from task STDERR", e);
        }

        return task;
    }

}
