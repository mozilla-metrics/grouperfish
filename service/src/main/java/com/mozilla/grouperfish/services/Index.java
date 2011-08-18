package com.mozilla.grouperfish.services;

import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Namespace;
import com.mozilla.grouperfish.model.Query;


public interface Index {

    Iterable<Document> find(Namespace ns, Query query);

    Iterable<Query> resolve(Namespace ns, Query query);

}
