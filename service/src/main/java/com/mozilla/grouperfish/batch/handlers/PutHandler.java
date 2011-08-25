package com.mozilla.grouperfish.batch.handlers;

import static com.mozilla.grouperfish.batch.Helpers.resultsFilename;

import java.io.Reader;
import java.util.Map;

import com.mozilla.grouperfish.base.StreamTool;
import com.mozilla.grouperfish.batch.Fail;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.naming.Scope;
import com.mozilla.grouperfish.rest.ResultsResource;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.Grid;


/**
 * Put run results into results storage.
 */
public class PutHandler implements TaskHandler {


    private final FileSystem fs;
    private final Grid grid;

    public PutHandler(final Grid grid, final FileSystem fs) {
        this.grid = grid;
        this.fs = fs;
    }

    @Override
    public Task handle(final Task task) throws Fail {

        final String key = ResultsResource.key(task.transform().name(), task.query().name());
        final Map<String, String> results = new Scope(task.namespace(), grid).results();

        try {
            final Reader reader = fs.reader(resultsFilename(task));
            results.put(key, StreamTool.consume(reader));
        }
        catch (final Exception e) {
            throw new Fail(task, "Could not read results from filesystem.", e);
        }
        return task;
    }

}
