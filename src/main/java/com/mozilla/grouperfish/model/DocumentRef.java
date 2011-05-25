package com.mozilla.grouperfish.model;

public class DocumentRef implements Ref<Document> {

	public CollectionRef ownerRef() {
		return ownerRef_;
	}

	public String id() {
		return id_;
	}

	@Override
	public Class<Document> model() {
		return Document.class;
	}

	public DocumentRef(CollectionRef ownerRef, String id) {
		ownerRef_ = ownerRef;
		id_ = id;
	}

	private final CollectionRef ownerRef_;
	private final String id_;

}
