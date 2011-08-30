package com.mozilla.grouperfish.util.loader;

import com.mozilla.grouperfish.model.Document;


public class DocumentLoader extends Loader<Document> {

    public DocumentLoader(final String baseUrl, final String namespace) {
        super(baseUrl + "/documents/" + namespace);
    }

}
