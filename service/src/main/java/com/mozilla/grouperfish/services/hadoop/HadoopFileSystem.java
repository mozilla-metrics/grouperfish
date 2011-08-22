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

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.base.Result;


public class HadoopFileSystem implements com.mozilla.grouperfish.services.FileSystem {

    private final FileSystem hdfs;
    private final Path basePath;

    public HadoopFileSystem() {
        final String hdfsRoot = System.getProperty("grouperfish.hdfs.root", "grouperfish");
        try {
            hdfs = FileSystem.get(new Configuration());
            this.basePath = new Path(hdfsRoot);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String uri(final String relativePath) {
        return abs(relativePath).toUri().toString();
    }

    @Override
    public Result<String> removeRecursively(final String relativePath) {
        Assert.nonNull(relativePath);
        Assert.check(!relativePath.isEmpty());
        final Result<String> result = new Result<String>();
        try {
            final Path absolutePath = abs(relativePath);
            hdfs.delete(absolutePath, true);
            result.put(absolutePath.toString());
        } catch (IOException e) {
            result.error(e);
        }
        return result;
    }

    @Override
    public Result<String> makeDirectory(final String relativePath) {
        Assert.nonNull(relativePath);
        Assert.check(!relativePath.isEmpty());
        final Result<String> result = new Result<String>();
        try {
            final Path absolutePath = abs(relativePath);
            if (!hdfs.getFileStatus(absolutePath).isDir()) {
                return result.error("makeDirectory: Path exists but is not a directory!");
            }
            hdfs.mkdirs(absolutePath);
            result.put(absolutePath.toString());
        } catch (IOException e) {
            result.error(e);
        }
        return result;
    }

    @Override
    public Result<Writer> writer(final String path) {
        Assert.nonNull(path);
        Assert.check(!path.isEmpty());
        final Result<Writer> result = new Result<Writer>();
        final Path filePath = abs(path);
        final FSDataOutputStream out;
        try {
            out = (hdfs.exists(filePath)) ? hdfs.append(filePath) : hdfs.create(filePath);
            result.put(new OutputStreamWriter(out));
        }
        catch (IOException e) {
            result.error(e);
        }
        return result;
    }

    @Override
    public Result<Reader> reader(String path) {
        final Result<Reader> result = new Result<Reader>();
        Assert.nonNull(path);
        Assert.check(!path.isEmpty());
        final Path filePath = abs(path);
        try {
            if (!hdfs.exists(filePath)) {
                return result.error("Path to read does not exist: '" + filePath + "'");
            }
            result.put(new InputStreamReader(hdfs.open(filePath)));
        }
        catch (IOException e) {
            result.error(e);
        }
        return result;
    }

    private Path abs(final String relativePath) {
        return new Path(basePath, relativePath);
    }

}
