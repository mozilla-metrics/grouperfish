package com.mozilla.grouperfish.batch.handlers;

import static com.mozilla.grouperfish.batch.handlers.Helpers.inputFilename;
import static com.mozilla.grouperfish.batch.handlers.Helpers.parametersFilename;
import static com.mozilla.grouperfish.batch.handlers.Helpers.writer;

import java.io.IOException;
import java.io.Writer;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.batch.RetryException;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.json.TsvJsonWriter;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.Index;

public class FetchHandler implements TaskHandler {

    private final Index index;
    private final FileSystem fs;

    public FetchHandler(final FileSystem fs, final Index index) {
        this.fs = fs;
        this.index = index;
    }

    @Override
    public Task handle(final Task task) throws RetryException {
        Assert.nonNull(task);
        final TsvJsonWriter tsvWriter = new TsvJsonWriter(writer(fs, task, inputFilename(task)));
        final Writer parametersWriter = writer(fs, task, parametersFilename(task));
        try {
            for (final Document doc : matches(task)) tsvWriter.write(doc);
            parametersWriter.write(task.transform().parametersJson());
        }
        catch (final IOException e) {
            final String message = String.format(
                    "Failed writing doc to %s", Helpers.inputFilename(task));
            throw new RetryException(message, e);
        }
        return task;
    }

    private Iterable<Document> matches(final Task task) throws RetryException {
        final Iterable<Document> matches = index.find(task.namespace(), task.query());
        return matches;
    }
}
