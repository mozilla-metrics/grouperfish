package com.mozilla.grouperfish.loader;

import com.mozilla.grouperfish.json.Converters;
import com.mozilla.grouperfish.model.Document;

//:TODO: Unit Test
public class DocumentLoader extends Loader<Document> {

    public DocumentLoader(final String namespace) {
        super(namespace, Converters.forDocuments());
    }

}
