package com.mozilla.grouperfish.services;

import java.io.Reader;
import java.io.Writer;


/**
 * A (virtual) file system root with the essential primitives for
 * Grouperfish batch operation:
 * - create/remove directories
 * - read/write textual data (tsv, json)
 * - obtain global uris that can be passed to external components.
 *
 * All operations except {@link #uri(String)} work with relative
 * (virtual) paths that are only meaningful to this filesystem
 * instance (in-memory fs, temp fs) or instances created with the same
 * parameters (local fs, distributed fs).
 */
public interface FileSystem {

    /** Result: The absolute path that was removed. */
    String removeRecursively(String relativePath) throws Denied, NotFound;

    /**
     * Creates the given directory if it does not exist already.
     * Fails if path exists but is not a directory.
     * @return The uri of the directory that was created.
     */
    String makeDirectory(String relativePath) throws Denied;

    /**
     * Opens a file for writing (creates the file if not present).
     * @return A suitable writer for string data.
     */
    Writer writer(String path) throws Denied;

    /**
     * Opens a file for writing (creates the file if not present).
     * @param path The filesystem local path.
     * @return A suitable reader for string data.
     */
    Reader reader(String path) throws Denied, NotFound;

    /**
     * Generate a url that can be used to reference this relative path externally.
     * Ensures that the referee actually exists (at least currently).
     */
    String uri(String path) throws NotFound;

    public static class FsError extends Exception {
        public FsError(final String message) { super(message); }
        public FsError(final String message, final Exception reason) { super(message, reason); }
        private static final long serialVersionUID = 1L;
    };

    public static class Denied extends FsError {
        public Denied(final String more) { super("Denied: " + more); }
        public Denied(final String more, final Exception reason) { super("Denied: " + more, reason); }
        private static final long serialVersionUID = 1L;
    };

    public static class NotFound extends FsError {
        public NotFound(final String uri) { super("Not found: " + uri); }
        public NotFound(final String uri, final Exception reason) { super("Not found: " + uri, reason); }
        private static final long serialVersionUID = 1L;
    };

}
