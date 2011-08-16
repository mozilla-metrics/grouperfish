package com.mozilla.grouperfish.json;


/** Converts some type to/from json. */
public interface JsonConverter<T> {
	String encode(final T item);
	T decode(final String json);
}
