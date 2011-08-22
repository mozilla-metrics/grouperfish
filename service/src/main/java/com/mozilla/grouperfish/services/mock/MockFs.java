package com.mozilla.grouperfish.services.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.mozilla.grouperfish.base.ArrayTool;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.base.Result;
import com.mozilla.grouperfish.services.FileSystem;


public class MockFs implements FileSystem {

    private final String root = "/mockfs";

    private final Map<String, byte[]> files = new Hashtable<String, byte[]>();

    @Override
    public Result<String> removeRecursively(final String relativePath) {
        Assert.nonNull(relativePath);
        Assert.check(!relativePath.isEmpty());
        final String path = abs(relativePath);

        final List<String> toRemove = new ArrayList<String>();
        for (final String key : files.keySet()) {
            if (!key.startsWith(path)) continue;
            final String rest = key.substring(key.length());
            if (rest.startsWith("/") || path.endsWith("/") || rest.isEmpty()) {
                toRemove.add(key);
            }
        }
        for (final String key : toRemove) files.remove(key);
        return new Result<String>(path);
    }

    @Override
    public Result<String> makeDirectory(final String relativePath) {
        Assert.nonNull(relativePath);
        Assert.check(!relativePath.isEmpty());
        return new Result<String>(abs(relativePath));
    }

    @Override
    public Result<Writer> writer(final String path) {
        return new Result<Writer>(new OutputStreamWriter(new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                if (files.containsKey(path)) {
                    files.put(path, ArrayTool.concat(files.get(path), toByteArray()));
                }
                else {
                    files.put(path, toByteArray());
                }
            }
        }));
    }

    @Override
    public Result<Reader> reader(final String path) {
        final Result<Reader> result = new Result<Reader>();
        if (!files.containsKey(path)) {
            result.error(String.format("Not found in mock fs: '%s'", path));
            return result;
        }
        result.put(new InputStreamReader(new ByteArrayInputStream(files.get(path))));
        return result;
    }

    @Override
    public String uri(final String relativePath) {
        return "mockfs://" + abs(relativePath);
    }

    private String abs(final String relativePath) {
        return root + (relativePath.startsWith("/") ? ""  : "/" ) + relativePath;
    }
}
