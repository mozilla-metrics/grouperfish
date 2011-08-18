package com.mozilla.grouperfish.batch.handlers;

import java.io.Writer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.mozilla.grouperfish.base.Result;
import com.mozilla.grouperfish.batch.RetryException;
import com.mozilla.grouperfish.batch.Task;
import com.mozilla.grouperfish.services.FileSystem;

class Helpers {

    /** Directory names use system locale for date formatting. */
    private static final DateTimeFormatter dateFormatter =
        DateTimeFormat.forPattern("YYYY-MM-dd").withLocale(Locale.getDefault());

    /** Directory names use system locale for date formatting. */
    private static final DateTimeFormatter timeFormatter =
        DateTimeFormat.forPattern("HH-mm-ss-SSS").withLocale(Locale.getDefault());

    private static final Pattern nonAlnum = Pattern.compile("[^A-Za-z0-9-]");

    /** Gives a working directory for the given task, relative to the grouperfish root directory. */
    static String taskDirectory(final Task task) {
        final String queryName = task.query().name();
        final String taskName = task.query().name();
        return String.format("tasks_%s/%s/%s-T%s-Q%s",
                             task.namespace().toString(),
                             dateFormatter.print(task.created()),
                             timeFormatter.print(task.created()),
                             mangle(taskName),
                             mangle(queryName));
    }

    static Writer writer(final FileSystem fs, final Task task, final String filename) throws RetryException {
        fs.makeDirectory(Helpers.taskDirectory(task));
        final Result<Writer> box = fs.writer(filename);
        for (final Exception e : box.error()) {
            final String message = String.format(
                    "Could not open %s for writing.", filename);
            throw new RetryException(message, e);
        }
        return box.get();
    }


    static String parametersFilename(final Task task) {
        return taskFilename(task, "parameters.json");
    }

    static String inputFilename(final Task task) {
        return taskFilename(task, "input.json");
    }

    static String resultsFilename(final Task task) {
        return taskFilename(task, "output/results.json");
    }

    static String tagsFilename(final Task task) {
        return taskFilename(task, "output/tags.json");
    }

    static String taskFilename(final Task task, final String filename) {
        return taskDirectory(task) + "/" + filename;
    }

    static String mangle(final String s) {
        return nonAlnum.matcher(s).replaceAll("_");
    }

}
