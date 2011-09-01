package com.mozilla.grouperfish.services.local;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.base.StreamTool;
import com.mozilla.grouperfish.services.api.FileSystem;

import static java.lang.String.format;

public class LocalFileSystem implements FileSystem {

    private static final Logger log = LoggerFactory.getLogger(LocalFileSystem.class);

    private final File rootDir;
    private final String rootPath;

    public LocalFileSystem(final String root) {
        Assert.nonNull(root);
        Assert.check(!root.isEmpty());
        rootDir = new File(root);
        Assert.check(!rootDir.exists() || rootDir.isDirectory());

        try {
            if (!rootDir.exists()) rootDir.mkdirs();
            rootPath = rootDir.getCanonicalPath();
        }
        catch (final IOException e) {
            throw new RuntimeException(root);
        }

        log.info("Instantiated service: {} (rootPath={})", getClass().getSimpleName(), rootPath);
    }

    @Override
    public String removeRecursively(final String relativePath) throws Denied, NotFound {
        final File toRemove = resolve(relativePath);
        removeRecursively(toRemove);
        return uri(relativePath);
    }

    @Override
    public String makeDirectory(final String relativePath) throws Denied {
        final File toCreate = resolve(relativePath);
        try {
            toCreate.mkdirs();
            return "file://" + toCreate.getCanonicalPath();
        }
        catch (final Exception e) {
            throw new Denied(format("Error while creating '%s'", relativePath), e);
        }
    }

    @Override
    public Writer writer(final String relativePath) throws Denied {
        return new BufferedWriter(new OutputStreamWriter(outStream(relativePath)));
    }

    @Override
    public Reader reader(final String relativePath) throws Denied, NotFound {
        return new BufferedReader(new InputStreamReader(
                inStream(relativePath), StreamTool.UTF8));
    }

    @Override
    public String uri(final String relativePath) throws NotFound {
        try {
            return "file://" + resolve(relativePath).getCanonicalPath();
        }
        catch (final Exception e) {
            throw new NotFound(format("Error while lookig up '%s'", relativePath), e);
        }
    }


    private void removeRecursively(final File file) throws Denied, NotFound {
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) removeRecursively(child);
        }
        if (file.delete()) throw new Denied(format("Failed to delete file '%s'", file.getAbsolutePath()));
    }

    private File resolve(final String relativePath) throws Denied {
        try {
            final File f = new File(rootDir, relativePath).getCanonicalFile();
            final String path = f.getCanonicalPath();
            if (!path.startsWith(rootPath))
                throw new Denied(format(
                        "Cannot resolve '%s': all paths must be local to the fs root.",
                        relativePath));
            return f;
        }
        catch (final IOException e) {
            throw new Denied(format("Could not resolve '%s' due to IO error.", relativePath), e);
        }
    }

    private final OutputStream outStream(final String relativePath) throws Denied {
        final File file = resolve(relativePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (final IOException e) {
                throw new Denied(format("Could not create file '%s'", relativePath));
            }
        }
        if (!file.canWrite()) {
            throw new Denied(format("Cannot write to '%s'", relativePath));
        }
        try {
            return new FileOutputStream(file);
        }
        catch (FileNotFoundException e) {
            throw new Denied(format("Unexpected: File '%s' vanished.", relativePath));
        }

    }

    private final InputStream inStream(final String relativePath) throws Denied, NotFound {
        final File file = resolve(relativePath);
        if (!file.canRead()) throw new Denied(format("Cannot read '%s'", relativePath));
        try {
            return new FileInputStream(file);
        }
        catch (final FileNotFoundException e) {
            throw new NotFound(format("Cannot find '%s'", relativePath), e);
        }
    }
}
