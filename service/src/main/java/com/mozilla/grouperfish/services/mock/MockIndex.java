package com.mozilla.grouperfish.services.mock;

import java.util.List;

import org.elasticsearch.common.collect.ImmutableList;

import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.naming.Namespace;
import com.mozilla.grouperfish.services.api.Index;

public class MockIndex implements Index {

    // Chosen by fair dice roll.
    private final List<Document> randomDocuments =
        new ImmutableList.Builder<Document>().
            add(new Document("A", "{\"id\": \"A\", \"text\": \"Some random text.\"}")).
            add(new Document("B", "{\"id\": \"B\", \"text\": \"Another text which is completely random.\"}")).
            add(new Document("C", "{\"id\": \"C\", \"text\": \"Only an ape with typewriter could think of this.\"}")).
            build();

    private final List<Query> randomQueries =
        new ImmutableList.Builder<Query>().
            add(new Query("A", "{\"query\": {\"field\": {\"x\": \"some\"}}}")).
            add(new Query("B", "{\"query\": {\"field\": {\"x\": \"thing\"}}}")).
            build();

    @Override
    public Iterable<Document> find(final Namespace ns, final Query query) {
        return randomDocuments;
    }

    @Override
    public Iterable<Query> resolve(Namespace ns, Query query) {
        return randomQueries;
    }

}
