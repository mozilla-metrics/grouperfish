package com.mozilla.grouperfish.model;

import java.util.Map;

import com.mozilla.grouperfish.base.Assert;


/** Simple multi-field text document. Each document has at least id and (full) text. */
public class Document implements Entity {

	private final String id_;

	private final Map<String, ? extends Object> fields_;

	/** Create a document from field mappings. Must contain non-null "id" field. */
	public Document(final Map<String, ? extends Object> fields) {
        Assert.check(fields.containsKey("id"));
	    id_ = String.valueOf(fields.get("id"));
	    fields_ = fields;
	}

	/** A unique identifier for this document. */
	public String id() { return id_; }

	public Map<String, ? extends Object> fields() {
	    return fields_;
	}

}
