package com.mozilla.grouperfish.services;

import java.io.Reader;
import java.io.Writer;

import com.mozilla.grouperfish.base.Result;


public interface FileSystem {

    /** Result: The absolute path that was removed. */
    Result<String> removeRecursively(String relativePath);

    /** Result: The absolute path that was created. */
    Result<String> createWithParents(String relativePath);

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

}
