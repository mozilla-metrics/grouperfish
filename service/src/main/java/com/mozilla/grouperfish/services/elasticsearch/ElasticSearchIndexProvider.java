package com.mozilla.grouperfish.services.elasticsearch;

import com.mozilla.grouperfish.services.api.Index;
import com.mozilla.grouperfish.services.api.IndexProvider;

public class ElasticSearchIndexProvider implements IndexProvider {

    @Override
    public Index index(String name) {
        return new ElasticSearchIndex(name);
    }

}
