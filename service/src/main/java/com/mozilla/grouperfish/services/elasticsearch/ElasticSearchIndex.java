package com.mozilla.grouperfish.services.elasticsearch;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
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
        return new Iterable<Document>() {
            @Override
            public Iterator<Document> iterator() {
                return new Pager(query);
            }

        };
    }

    @Override
    public Iterable<Query> resolve(Query query) {
        if (!query.isTemplate()) return ImmutableList.of(query);
        // :TODO: Template queries
        Assert.unreachable("Not implemented.");
        return null;
    }


    private static final int BATCH_SIZE = 2048;
    private static final TimeValue TIMEOUT = TimeValue.timeValueSeconds(30);

    class Pager implements Iterator<Document> {

        final Query query;
        final String scrollId;
        final long totalHits;

        long position = 0;
        int positionInBatch = 0;
        SearchHits batch = null;

        public Pager(final Query query) {
            this.query = query;

            final SearchResponse response = client.prepareSearch(indexName)
                    .setTypes(type)
                    .setSize(BATCH_SIZE)
                    .setScroll(TIMEOUT)
                    .setExtraSource(query.source()).execute().actionGet();

            batch = response.hits();

            scrollId = response.getScrollId();
            totalHits = batch.getTotalHits();

            log.debug(String.format("First batch: got %s/%s documents (index: %s, type: %s, query: %s)...",
                                    batch.hits().length, totalHits, indexName, type, query.name()));
            log.trace("Query: {}", query.source());
        }

        @Override
        public boolean hasNext() {
            return position < totalHits;
        }

        @Override
        public Document next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Tried to get more docs than available.");
            }
            if (positionInBatch == BATCH_SIZE) {
                final SearchResponse response =
                        client.prepareSearchScroll(scrollId).setScroll(TIMEOUT).execute().actionGet();
                batch = response.hits();
                positionInBatch = 0;
                log.debug(String.format("Next batch: got %s/%s documents (index: %s, type: %s, query: %s)...",
                                        position + batch.hits().length, totalHits, indexName, type, query.name()));

            }
            final SearchHit next = batch.getAt(positionInBatch);
            ++positionInBatch;
            ++position;
            return new Document(next.getId(), next.getSource());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
