package com.mozilla.grouperfish.services;

import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.naming.Namespace;


public interface Index {

    Iterable<Document> find(Namespace ns, Query query);

    Iterable<Query> resolve(Namespace ns, Query query);

}
