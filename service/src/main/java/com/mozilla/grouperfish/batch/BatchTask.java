package com.mozilla.grouperfish.batch;

import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.model.TransformConfig;

public class BatchTask {

    private final Query query;
    private final TransformConfig transform;

    public BatchTask(final Query query, final TransformConfig transform) {
        this.query = query;
        this.transform = transform;
    }

    public Query query() {
        return query;
    }

    public TransformConfig transform() {
        return transform;
    }

}
