package com.mozilla.grouperfish.batch.scheduling;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.mozilla.grouperfish.batch.handlers.CleanupHandler;
import com.mozilla.grouperfish.batch.handlers.FetchHandler;
import com.mozilla.grouperfish.batch.handlers.PutHandler;
import com.mozilla.grouperfish.batch.handlers.RunHandler;
import com.mozilla.grouperfish.batch.handlers.SequentialHandler;
import com.mozilla.grouperfish.batch.handlers.TaskHandler;
import com.mozilla.grouperfish.batch.transforms.TransformProvider;
import com.mozilla.grouperfish.model.Task;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Grid;
import com.mozilla.grouperfish.services.api.Index;
import com.mozilla.grouperfish.services.api.FileSystem.Denied;
import com.mozilla.grouperfish.services.api.FileSystem.NotFound;

public class Helpers {

    /** Gives a working directory for the given task, relative to the grouperfish root directory. */
    public static String taskDirectory(final Task task) {
        final String queryName = task.query().name();
        final String taskName = task.query().name();
        return String.format("tasks_%s/%s/%s-T%s-Q%s",
                             task.namespace().toString(),
                             dateFormatter.print(task.created()),
                             timeFormatter.print(task.created()),
                             mangle(taskName),
                             mangle(queryName));
    }

    public static Writer writer(final FileSystem fs, final Task task, final String filename) throws Denied {
        fs.makeDirectory(Helpers.taskDirectory(task));
        return fs.writer(filename);
    }

    /**
     * Copy file as simply (and inefficiently) as possible.
     * :TODO: Expose I/O streams to make this somewhat efficient.
     */
    public static void copy(
            final String relativePath,
            final FileSystem from,
            final FileSystem to) throws NotFound, Denied, IOException {
        final Reader source = from.reader(relativePath);
        final Writer dest = to.writer(relativePath);
        int c = source.read();
        while (c != -1) {
            dest.write(c);
            c = source.read();
        }
        source.close();
        dest.close();
    }

    public static String parametersFilename(final Task task) {
        return taskFilename(task, "parameters.json");
    }

    public static String inputFilename(final Task task) {
        return taskFilename(task, "input.json");
    }

    public static String resultsFilename(final Task task) {
        return taskFilename(task, "output/results.json");
    }

    public static String tagsFilename(final Task task) {
        return taskFilename(task, "output/tags.json");
    }

    public static String taskFilename(final Task task, final String filename) {
        return taskDirectory(task) + "/" + filename;
    }

    public static String mangle(final String s) {
        return nonAlnum.matcher(s).replaceAll("_");
    }

    /**
     * Produce a composite handler that performs all steps of task
     * execution.
     */
    static TaskHandler sequentialHandler(
            final Grid grid,
            final FileSystem fs,
            final Index index,
            final TransformProvider transforms) {
        return new SequentialHandler(
                new FetchHandler(fs, index),
                new RunHandler(fs, transforms),
                new PutHandler(grid, fs),
                new CleanupHandler(fs));
    }

    /** Directory names use system locale for date formatting. */
    private static final DateTimeFormatter dateFormatter =
        DateTimeFormat.forPattern("YYYY-MM-dd").withLocale(Locale.getDefault());

    /** Directory names use system locale for date formatting. */
    private static final DateTimeFormatter timeFormatter =
        DateTimeFormat.forPattern("HH-mm-ss-SSS").withLocale(Locale.getDefault());

    private static final Pattern nonAlnum = Pattern.compile("[^A-Za-z0-9-]");

}
