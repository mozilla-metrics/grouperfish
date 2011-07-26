package com.mozilla.grouperfish.bagheera;

import com.mozilla.grouperfish.json.Converters;
import com.mozilla.grouperfish.model.Document;

public class DocumentLoader extends Loader<Document> {

    public DocumentLoader(final String namespace) {
        super(namespace, Converters.forDocuments());
    }

}
