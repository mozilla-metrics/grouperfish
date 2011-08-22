package com.mozilla.grouperfish.model;

import java.util.Map;

import com.mozilla.grouperfish.base.Assert;


/** Simple multi-field text document. Each document has at least id and (full) text. */
public class Document extends NamedSource {

    public Document(final String id, final String source) {
	    super(id, source);
	}

    public Document(final String id, final Map<String, ? extends Object> fields) {
        super(id, fields);
    }

    public Document(final Map<String, ? extends Object> fields) {
        super(String.valueOf(fields.get("id")), fields);
        Assert.nonNull(fields.get("id"));
    }

	/**
	 * For documents this is the same as name.
	 */
	public String id() {
	    return name();
	}

    private static final long serialVersionUID = 0;

}
