package com.mozilla.grouperfish.batch.handlers;

import static com.mozilla.grouperfish.batch.scheduling.Helpers.inputFilename;
import static com.mozilla.grouperfish.batch.scheduling.Helpers.parametersFilename;
import static com.mozilla.grouperfish.batch.scheduling.Helpers.writer;

import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.base.json.TsvJsonWriter;
import com.mozilla.grouperfish.batch.scheduling.Helpers;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Fail;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.model.Type;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Index;
import com.mozilla.grouperfish.services.api.IndexProvider;

public class FetchHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(FetchHandler.class);

    private final IndexProvider indexes;
    private final FileSystem fs;

    public FetchHandler(final FileSystem fs, final IndexProvider index) {
        this.fs = fs;
        this.indexes = index;
    }

    @Override
    public Task handle(final Task task) throws Fail {
        Index index = indexes.index(task.namespace().name(Type.DOCUMENT));
        Assert.nonNull(task);
        try {
            final TsvJsonWriter tsvWriter = new TsvJsonWriter(writer(fs, task, inputFilename(task)));
            for (final Document doc : index.find(task.query())) tsvWriter.write(doc);
            tsvWriter.close();

            final Writer parametersWriter = writer(fs, task, parametersFilename(task));
            parametersWriter.write(task.transform().parametersJson());
            parametersWriter.close();
        }
        catch (final Exception e) {
            final String message = String.format(
                    "Failed writing doc to %s", Helpers.inputFilename(task));
            log.error("Exception", e);
            throw new Fail(task, message, e);
        }
        return task;
    }

}
