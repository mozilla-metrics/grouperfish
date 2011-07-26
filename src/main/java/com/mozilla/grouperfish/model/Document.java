package com.mozilla.grouperfish.model;

import java.util.Map;

import com.mozilla.grouperfish.base.Assert;


/** Simple multi-field text document. Each document has at least id and (full) text. */
public class Document implements Entity {

	private final String id_;

	private final String text_;

	private final Map<String, Object> fields_;

	public Document(final String id, final String text, final Map<String, Object> fields) {
		Assert.nonNull(id, fields);
		id_ = id;
		text_ = text;
		fields_ = fields;
	}

	/** Create a document from field mappings. Must contain non-null "id" and "text" fields. */
	public Document(final Map<String, Object> fields) {
	    id_ = String.valueOf(fields.get("id"));
	    text_ = String.valueOf(fields.get("text"));
	    fields_ = fields;
	}

	/** A unique identifier for this document. */
	public String id() { return id_; }

	/** A full-text representation of the document. Relevant for clustering. */
	public String text() { return text_; }

	/** All fields (possibly including text/id). */
	public Object field(final String name) {
		Assert.nonNull(name);
		return fields_.get(name);
	}

	public Map<String, Object> fields() {
	    return fields_;
	}

}
