package com.mozilla.grouperfish.batch.handlers;

import java.io.Reader;

import com.mozilla.grouperfish.base.Result;
import com.mozilla.grouperfish.batch.RetryException;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.Grid;

import static com.mozilla.grouperfish.batch.handlers.Helpers.resultsFilename;


/**
 * Loads run results into storage.
 */
public class PutHandler implements TaskHandler {


    private final FileSystem fs;

    public PutHandler(final Grid grid, final FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public Task handle(final Task task) throws RetryException {

        final Result<Reader> result = fs.reader(resultsFilename(task));
        for (final Exception error : result.error())
            throw new RetryException("Could not read results from filesystem:", error);

        return null;
    }

}
