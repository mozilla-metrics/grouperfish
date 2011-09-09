package com.mozilla.grouperfish.services.api;

import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Query;


public interface Index {

    Iterable<Document> find(Query query);

    Iterable<Query> resolve(Query query);

}
