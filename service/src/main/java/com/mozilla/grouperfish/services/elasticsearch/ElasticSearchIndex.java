package com.mozilla.grouperfish.services.elasticsearch;

import org.elasticsearch.client.transport.TransportClient;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Namespace;
import com.mozilla.grouperfish.model.Query;

public class ElasticSearchIndex implements com.mozilla.grouperfish.services.Search {

    public ElasticSearchIndex() {
        TransportClient client;

    }

    @Override
    public Iterable<Document> find(final Namespace ns, final Query query) {
        return Assert.unreachable(Iterable.class, "not implemented");
    }

}
