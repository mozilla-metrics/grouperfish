package com.mozilla.grouperfish.services.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.search.SearchRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Query;


public class ElasticSearchIndex implements com.mozilla.grouperfish.services.api.Index {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndex.class);

    private final String indexName;
    private final String type;
    private final Client client;

    public ElasticSearchIndex(final Client client, final String indexName, final String type) {
        this.client = client;
        this.indexName = indexName;
        this.type = type;
    }

    @Override
    public Iterable<Document> find(final Query query) {

        final SearchRequestBuilder requestBuilder =
            client.prepareSearch(indexName).setTypes(type).setExtraSource(query.source());
        final SearchRequest request = requestBuilder.request();
        final SearchResponse response = client.search(request).actionGet();

        // :TODO: Optimize:
        // Smarter implementation of the iterable would allow to spit results from scroller...
        final List<Document> results = new ArrayList<Document>();
        for (final SearchHit hit : response.hits()) {
            results.add(new Document(hit.getId(), hit.getSource()));
        }

        log.debug(String.format("Returning %s/%s documents from find (index: %s, type: %s, query: %s)...",
                                results.size(), response.hits().totalHits(), indexName, type, query.source()));
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
