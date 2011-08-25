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
import com.mozilla.grouperfish.services.FileSystem;


public class MockFs implements FileSystem {

    private final String root;

    private final Map<String, byte[]> files = new Hashtable<String, byte[]>();

    public MockFs(final String root) {
        this.root = root;
    }

    @Override
    public synchronized String removeRecursively(final String relativePath) throws Denied, NotFound {
        Assert.nonNull(relativePath);
        Assert.check(!relativePath.isEmpty());

        final List<String> toRemove = new ArrayList<String>();
        for (final String key : files.keySet()) {
            if (!key.startsWith(relativePath)) continue;
            final String rest = key.substring(key.length());
            if (rest.startsWith("/") || relativePath.endsWith("/") || rest.isEmpty()) {
                toRemove.add(key);
            }
        }
        for (final String key : toRemove) files.remove(key);
        return uncheckedUri(relativePath);
    }

    @Override
    public synchronized String makeDirectory(final String relativePath) throws Denied {
        Assert.nonNull(relativePath);
        Assert.check(!relativePath.isEmpty());
        if (files.containsKey(relativePath)) throw new Denied("used as file: " + uncheckedUri(relativePath));
        return uncheckedUri(relativePath);
    }

    @Override
    public synchronized Writer writer(final String path) throws Denied {
        return new OutputStreamWriter(new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                if (files.containsKey(path)) {
                    files.put(path, ArrayTool.concat(files.get(path), toByteArray()));
                }
                else {
                    files.put(path, toByteArray());
                }
            }
        });
    }

    @Override
    public synchronized Reader reader(final String path) throws Denied, NotFound {
        if (!files.containsKey(path)) throw new NotFound(uri(path));
        return new InputStreamReader(new ByteArrayInputStream(files.get(path)));
    }

    @Override
    public String uri(final String relativePath) throws NotFound {
        if (!files.containsKey(relativePath)) throw new NotFound(relativePath);
        return uncheckedUri(relativePath);
    }

    private String uncheckedUri(final String relativePath) {
        return "mockfs://" + root + (relativePath.startsWith("/") ? ""  : "/" ) + relativePath;
    }
}
