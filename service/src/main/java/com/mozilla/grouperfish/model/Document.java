package com.mozilla.grouperfish.model;

import java.util.Map;

import com.mozilla.grouperfish.base.Assert;


/** Simple multi-field text document. Each document has at least id and (full) text. */
//:TODO: Unit Test
public class Document implements Entity {

	private final String id_;

	private final Map<String, Object> fields_;

	public Document(final String id, final Map<String, Object> fields) {
		Assert.nonNull(id, fields);
		id_ = id;
		fields_ = fields;
	}

	/** Create a document from field mappings. Must contain non-null "id" field. */
	public Document(final Map<String, Object> fields) {
	    id_ = String.valueOf(fields.get("id"));
	    Assert.nonNull(id_);
	    fields_ = fields;
	}

	/** A unique identifier for this document. */
	public String id() { return id_; }

	/** All fields (possibly including text/id). */
	public Object field(final String name) {
		Assert.nonNull(name);
		return fields_.get(name);
	}

	public Map<String, Object> fields() {
	    return fields_;
	}

}
