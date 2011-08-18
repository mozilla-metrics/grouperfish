package com.mozilla.grouperfish.batch.handlers;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.mozilla.grouperfish.batch.RetryException;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.model.TransformConfig;
import com.mozilla.grouperfish.services.FileSystem;

public class RunHandler implements TaskHandler {

    private static Logger log = LoggerFactory.getLogger(RunHandler.class);
    private final FileSystem fs;

    public RunHandler(final FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public Task handle(final Task task) throws RetryException {
        final String uri = fs.uri(Helpers.taskDirectory(task));
        final TransformConfig config = task.transform();

        final Map<String, String> knownTransforms =
            new ImmutableMap.Builder<String, String>().put("coclustering", "./transforms/coclustering/coclustering").build();

        // :TODO: Implement
        // - create local working directory
        // - get transform name and real path (USE WHITELIST <--- security)
        // - for local execution, copy input and parameters from HDFS to local FS
        // - invoke transform command (locally or mapreduce)

        final String transform = knownTransforms.get(config.transform());
        try {
            log.info("Launching transform '%s' with input directory '%s'", transform, uri);
            new ProcessBuilder(transform, uri).directory(new File("the-local-working-directory-uri")).start();
        } catch (IOException e) {
            throw new RetryException("IO failed during execution.", e);
        }
        return task;
    }

}
