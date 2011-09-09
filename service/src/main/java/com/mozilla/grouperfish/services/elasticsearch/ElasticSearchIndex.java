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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Query;


public class ElasticSearchIndex implements com.mozilla.grouperfish.services.api.Index {

    public static final String PROPERTY_CLUSTER = "grouperfish.services.elasticsearch.cluster";
    public static final String PROPERTY_CLUSTER_DEFAULT = "grouperfish";

    public static final String PROPERTY_TYPE = "grouperfish.services.elasticsearch.type";
    public static final String PROPERTY_TYPE_DEFAULT = "data";

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndex.class);

    private final Client client;
    private final String indexName;
    private final String type;

    public ElasticSearchIndex(final String name) {
        this.indexName = name;
        type = System.getProperty(PROPERTY_TYPE, PROPERTY_TYPE_DEFAULT);
        final String clusterName = System.getProperty(PROPERTY_CLUSTER, PROPERTY_CLUSTER_DEFAULT);
        Node node = NodeBuilder.nodeBuilder().client(true).clusterName(clusterName).build();
        client = node.client();
        log.info("Instantiated service: {} (clusterName={})", getClass().getSimpleName(), clusterName);
    }

    @Override
    public Iterable<Document> find(final Query query) {
        final SearchRequestBuilder requestBuilder =
            client.prepareSearch(indexName).setTypes(type).setSource(query.source());
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
    public Iterable<Query> resolve(Query query) {
        if (!query.isTemplate()) return ImmutableList.of(query);
        // :TODO: Template queries
        Assert.unreachable("Not implemented.");
        return null;
    }

}
