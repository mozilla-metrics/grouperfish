package com.mozilla.grouperfish.batch.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.batch.scheduling.Helpers;
import com.mozilla.grouperfish.batch.transforms.Transform;
import com.mozilla.grouperfish.batch.transforms.TransformProvider;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.model.TransformConfig;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.FileSystem.NotFound;


/** Perform the actual running of the transform. */
public class RunHandler implements TaskHandler {

    private static Logger log = LoggerFactory.getLogger(RunHandler.class);
    private final FileSystem fs;
    private final TransformProvider transforms;

    public RunHandler(final FileSystem fs, final TransformProvider transforms) {
        this.fs = fs;
        this.transforms = transforms;
    }

    @Override
    public Task handle(final Task task) throws Fail {
        final String uri;
        try {
            uri = fs.uri(Helpers.taskDirectory(task));
        } catch (NotFound e) {
            throw new Fail(task, "Task input not found...");
        }

        final TransformConfig config = task.transform();
        final Transform transform = transforms.get(config.transform());
        Assert.nonNull(transform);
        log.info("Launching transform '%s' with input directory '%s'", transform, uri);
        // new ProcessBuilder(transform, uri).directory(new File("the-local-working-directory-uri")).start();
        return task;
    }

}
