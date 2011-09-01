package com.mozilla.grouperfish.batch.handlers;

import static com.mozilla.grouperfish.batch.scheduling.Helpers.inputFilename;
import static com.mozilla.grouperfish.batch.scheduling.Helpers.parametersFilename;
import static com.mozilla.grouperfish.batch.scheduling.Helpers.writer;

import java.io.Writer;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.base.json.TsvJsonWriter;
import com.mozilla.grouperfish.batch.scheduling.Helpers;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Index;

public class FetchHandler implements TaskHandler {

    private final Index index;
    private final FileSystem fs;

    public FetchHandler(final FileSystem fs, final Index index) {
        this.fs = fs;
        this.index = index;
    }

    @Override
    public Task handle(final Task task) throws Fail {
        Assert.nonNull(task);
        try {
            final TsvJsonWriter tsvWriter = new TsvJsonWriter(writer(fs, task, inputFilename(task)));
            for (final Document doc : matches(task)) tsvWriter.write(doc);
            tsvWriter.close();

            final Writer parametersWriter = writer(fs, task, parametersFilename(task));
            parametersWriter.write(task.transform().parametersJson());
            parametersWriter.close();
        }
        catch (final Exception e) {
            final String message = String.format(
                    "Failed writing doc to %s", Helpers.inputFilename(task));
            throw new Fail(task, message, e);
        }
        return task;
    }

    private Iterable<Document> matches(final Task task) throws Fail {
        return index.find(task.namespace(), task.query());
    }
}
