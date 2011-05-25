package com.mozilla.grouperfish.model;

public class CollectionRef implements Ref<Collection> {

	/** Namespace for the key. */
	public String namespace() {
		return namespace_;
	}

	/** The collection-key (a <em>part</em> of rowkeys). */
	public String key() {
		return key_;
	}

	@Override
	public Class<Collection> model() {
		return Collection.class;
	}

	public CollectionRef(String namespace, String key) {
		namespace_ = namespace;
		key_ = key;
	}

	private final String key_;
	private final String namespace_;

}
