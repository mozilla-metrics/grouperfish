package com.mozilla.grouperfish.services.hadoop;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;


public class HadoopFileSystem implements com.mozilla.grouperfish.services.api.FileSystem {

    public static final String PROPERTY_DFS_ROOT = "grouperfish.services.hadoop.dfs.root";

    private static final Logger log = LoggerFactory.getLogger(HadoopFileSystem.class);

    private final FileSystem hdfs;
    private final Path basePath;

    public HadoopFileSystem() {
        final String hdfsRoot = System.getProperty(PROPERTY_DFS_ROOT, "grouperfish");
        try {
            hdfs = FileSystem.get(new Configuration());
            this.basePath = new Path(hdfsRoot);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Instantiated service: {} (hdfsRoot={})", getClass().getSimpleName(), hdfsRoot);
    }

    @Override
    public String uri(final String relativePath) throws NotFound {
        final Path abs = abs(relativePath);
        try {
            if (!hdfs.exists(abs)) throw new NotFound(uncheckedUri(abs));
        }
        catch (IOException e) {
            throw new NotFound("IOException trying to check for existence of " + relativePath, e);
        }
        return abs.toUri().toString();
    }

    @Override
    public String removeRecursively(final String relativePath) throws Denied {
        Assert.nonNull(relativePath);
        Assert.check(!relativePath.isEmpty());
        try {
            final Path absolutePath = abs(relativePath);
            if (!hdfs.exists(absolutePath)) return uncheckedUri(absolutePath);
            hdfs.delete(absolutePath, true);
            return uncheckedUri(absolutePath);
        } catch (final IOException e) {
            throw new Denied("Error removing ", e);
        }
    }

    @Override
    public String makeDirectory(final String relativePath) throws Denied {
        Assert.nonNull(relativePath);
        Assert.check(!relativePath.isEmpty());
        final Path absolutePath = abs(relativePath);
        try {
            if (!hdfs.getFileStatus(absolutePath).isDir()) {
                final String message = String.format(
                        "Path %s exists but is not a directory!",
                        uncheckedUri(absolutePath));
                throw new Denied(message);
            }
            hdfs.mkdirs(absolutePath);
            return uncheckedUri(absolutePath);
        } catch (IOException e) {
            throw new Denied("Failed to create " + uncheckedUri(absolutePath), e);
        }
    }

    @Override
    public Writer writer(final String path) throws Denied {
        Assert.nonNull(path);
        Assert.check(!path.isEmpty());
        final Path filePath = abs(path);
        final FSDataOutputStream out;
        try {
            out = (hdfs.exists(filePath)) ? hdfs.append(filePath) : hdfs.create(filePath);
            return new OutputStreamWriter(out);
        }
        catch (final IOException e) {
            throw new Denied("Cannot write to " + uncheckedUri(filePath), e);
        }
    }

    @Override
    public Reader reader(String path) throws NotFound, Denied {
        Assert.nonNull(path);
        Assert.check(!path.isEmpty());
        final Path filePath = abs(path);
        try {
            if (!hdfs.exists(filePath)) {
                final String message = String.format(
                        "Path to read does not exist: '%s'",
                        uncheckedUri(filePath));
                throw new NotFound(message);
            }
            return new InputStreamReader(hdfs.open(filePath));
        }
        catch (final IOException e) {
            throw new Denied("Cannot read from " + uncheckedUri(filePath), e);
        }
    }

    private Path abs(final String relativePath) {
        return new Path(basePath, relativePath);
    }

    private String uncheckedUri(final Path absolutePath) {
        return absolutePath.toUri().toString();
    }

}
