package com.mozilla.grouperfish.services;

import java.io.Reader;
import java.io.Writer;

import com.mozilla.grouperfish.base.Result;


public interface FileSystem {

    /** Result: The absolute path that was removed. */
    Result<String> removeRecursively(String relativePath);

    /**
     * Creates the given directory if it does not exist already.
     * Fails if path exists but is not a directory.
     * Result: The absolute path that was created.
     */
    Result<String> makeDirectory(String relativePath);

    /**
     * Opens a file for writing (creates the file if not present).
     * Result: A suitable writer for string data.
     */
    Result<Writer> writer(String path);

    /**
     * Opens a file for writing (creates the file if not present).
     * Result: A suitable reader for string data.
     */
    Result<Reader> reader(String path);

    /** Generate a url that can be used to reference this relative path externally. */
    String uri(String path);

}
