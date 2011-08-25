package com.mozilla.grouperfish.batch.run;

import java.io.InputStream;

import com.mozilla.grouperfish.batch.Fail;
import com.mozilla.grouperfish.batch.Task;


/**
 * Proxy to the real transform implementation (which can be a java class, a local executable, a RPC call...).
 */
public interface Transform {

    public interface TransformResult {
        InputStream stderr();
        boolean success();
    }

    public TransformResult run(Task task) throws Fail, InterruptedException;

    public boolean requiresDfs();
}
