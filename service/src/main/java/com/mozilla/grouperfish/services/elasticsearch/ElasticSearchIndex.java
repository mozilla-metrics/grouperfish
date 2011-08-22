package com.mozilla.grouperfish.services.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;

import com.google.common.collect.ImmutableList;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.naming.Namespace;


public class ElasticSearchIndex implements com.mozilla.grouperfish.services.Index {

    private final String DOCUMENT_TYPE_NAME = "data";

    private final Client client;

    public ElasticSearchIndex() {
        final String clusterName = System.getProperty("grouperfish.elasticsearch.cluster", "grouperfish");
        Node node = NodeBuilder.nodeBuilder().client(true).clusterName(clusterName).build();
        client = node.client();
    }

    @Override
    public Iterable<Document> find(final Namespace ns, final Query query) {
        final SearchRequestBuilder requestBuilder =
            client.prepareSearch(ns.raw()).setTypes(DOCUMENT_TYPE_NAME).setSource(query.source());
        final SearchRequest request = requestBuilder.request();
        final SearchResponse response = client.search(request).actionGet();

        // :TODO: Optimize:
        // Smarter implementation of the iterable would allow to spit results from scroller...
        final List<Document> results = new ArrayList<Document>();
        for (final SearchHit hit : response.getHits()) {
            results.add(new Document(hit.getId(), hit.getSource()));
        }
        return results;
    }

    @Override
    public Iterable<Query> resolve(Namespace ns, Query query) {
        if (!query.isTemplate()) return ImmutableList.of(query);
        // :TODO: Template queries
        Assert.unreachable("Not implemented.");
        return null;
    }

}
