package com.mozilla.grouperfish.batch.handlers;

import static com.mozilla.grouperfish.batch.scheduling.Helpers.resultsFilename;

import java.io.Reader;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.StreamTool;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.naming.Scope;
import com.mozilla.grouperfish.rest.jaxrs.ResultsResource;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Grid;


/**
 * Put run results into results storage.
 */
public class PutHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(PutHandler.class);

    private final FileSystem fs;
    private final Grid grid;

    public PutHandler(final Grid grid, final FileSystem fs) {
        this.grid = grid;
        this.fs = fs;
    }

    @Override
    public Task handle(final Task task) throws Fail {

        if (!task.isOk()) {
            log.debug("Not putting result for failed task %s", task);
        }

        final String key = ResultsResource.key(task.transform().name(), task.query().name());
        final Map<String, String> results = new Scope(task.namespace(), grid).results();

        try {
            final Reader reader = fs.reader(resultsFilename(task));
            results.put(key, StreamTool.consume(reader));
        }
        catch (final Exception e) {
            throw Fail.hard(task, "Could not read results from filesystem.", e);
        }
        return task;
    }

}
