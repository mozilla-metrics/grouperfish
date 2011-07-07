package com.mozilla.grouperfish.hbase;

import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.hbase.Schema.Collections.Main;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.Collection.Attribute;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.Ref;


public class CollectionAdapter implements RowAdapter<Collection> {

	static final String DEFAULT = "DEFAULT";
	final Factory factory_;

	public CollectionAdapter(Factory factory) {
		factory_ = factory;
	}

	@Override
	public Put put(Collection collection) {

		final CollectionRef ref = collection.ref();
		final Put put = new Put(Bytes.toBytes(key(collection))).add(Main.FAMILY, Main.NAMESPACE.qualifier,
				Bytes.toBytes(ref.namespace())).add(Main.FAMILY, Main.KEY.qualifier, Bytes.toBytes(ref.key()));

		// Only attributes that were specified by the caller are stored.
		for (Attribute a : Collection.Attribute.values()) {
			if (collection.get(a) == null)
				continue;
			// :TODO: we should store any number as plain byte...
			// Currently we keep the text representation to help the REST service.
			byte[] representation = Bytes.toBytes(String.valueOf(collection.get(a)));
			switch (a) {
			case MODIFIED:
				put.add(Main.FAMILY, Main.MODIFIED.qualifier, representation);
				break;
			case SIZE:
				put.add(Main.FAMILY, Main.SIZE.qualifier, representation);
				break;
			case REBUILT:
				put.add(Main.FAMILY, Main.Configuration.REBUILT.qualifier(DEFAULT), representation);
				break;
			case PROCESSED:
				put.add(Main.FAMILY, Main.Configuration.PROCESSED.qualifier(DEFAULT), representation);
				break;
			default:
				Assert.unreachable("Unknown collection attribute: ", a.name());
			}
		}
		return put;
	}

	@Override
	public String key(Collection collection) {
		return factory_.keys().key(collection.ref());
	}

	@Override
	public Collection read(Result result) {

		if (result.isEmpty()) return null;

		final Map<byte[], byte[]> main;
		{
			final Map<byte[], NavigableMap<byte[], byte[]>> latest = result.getNoVersionMap();
			main = latest.get(Main.FAMILY);
		}

		final Collection c;
		{
			final byte[] ns = main.get(Main.NAMESPACE.qualifier);
			final byte[] key = main.get(Main.KEY.qualifier);
			final CollectionRef ref = new CollectionRef(Bytes.toString(ns), Bytes.toString(key));
			c = new Collection(ref);
		}

		maybeSet(c, Attribute.MODIFIED, main, Main.MODIFIED.qualifier);
		maybeSet(c, Attribute.SIZE, main, Main.SIZE.qualifier);
		maybeSet(c, Attribute.REBUILT, main, Main.Configuration.REBUILT.qualifier(DEFAULT));
		maybeSet(c, Attribute.PROCESSED, main, Main.Configuration.PROCESSED.qualifier(DEFAULT));

		return c;
	}

	@Override
	public Get get(Ref<Collection> ref) {
		Assert.check(ref instanceof CollectionRef);
		return get((CollectionRef) ref);
	}

	public Get get(CollectionRef ref) {
		return new Get(Bytes.toBytes(factory_.keys().key(ref)));
	}

	private void maybeSet(final Collection c, final Attribute attr, final Map<byte[], byte[]> main,
			final byte[] qualifier) {
		if (!main.containsKey(qualifier))
			return;
		c.set(attr, Long.valueOf(Bytes.toString(main.get(qualifier))));
	}

}