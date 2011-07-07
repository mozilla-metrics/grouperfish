package com.mozilla.grouperfish.hbase;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.hbase.Schema.Documents;
import com.mozilla.grouperfish.hbase.Schema.Documents.Main;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.DocumentRef;
import com.mozilla.grouperfish.model.Ref;

public class DocumentAdapter implements RowAdapter<Document> {

	final Factory factory_;

	public DocumentAdapter(Factory factory) {
		factory_ = factory;
	}

	@Override
	public Put put(Document doc) {
		if (doc.text().length() == 0)
			return null;
		CollectionRef owner = doc.ref().ownerRef();
		return new Put(Bytes.toBytes(key(doc)))
				.add(Documents.Main.FAMILY, Documents.Main.NAMESPACE.qualifier, Bytes.toBytes(owner.namespace()))
				.add(Documents.Main.FAMILY, Documents.Main.COLLECTION_KEY.qualifier, Bytes.toBytes(owner.key()))
				.add(Documents.Main.FAMILY, Documents.Main.ID.qualifier, Bytes.toBytes(doc.ref().id()))
				.add(Documents.Main.FAMILY, Documents.Main.TEXT.qualifier, Bytes.toBytes(doc.text()));
	}

	@Override
	public String key(Document doc) {
		return factory_.keys().key(doc);
	}

	public Source<Document> all(CollectionRef ref, long timestamp) {
		final Scan scan = new Scan();
		scan.setMaxVersions(1);
		final String prefix = factory_.keys().documentPrefix(ref);
		scan.setFilter(new PrefixFilter(Bytes.toBytes(prefix)));
		try {
			scan.setTimeRange(0, timestamp);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		return new Source<Document>(factory_, Document.class, scan);
	}

	@Override
	public Document read(Result result) {

		final Map<byte[], byte[]> columns;
		{
			final Map<byte[], NavigableMap<byte[], byte[]>> latest = result.getNoVersionMap();
			columns = latest.get(Main.FAMILY);
		}

		final Document d;
		{
			final byte[] ns = columns.get(Main.NAMESPACE.qualifier);
			final byte[] key = columns.get(Main.COLLECTION_KEY.qualifier);
			final byte[] id = columns.get(Main.ID.qualifier);
			final byte[] text = columns.get(Main.TEXT.qualifier);
			final CollectionRef ref = new CollectionRef(Bytes.toString(ns), Bytes.toString(key));
			d = new Document(new DocumentRef(ref, Bytes.toString(id)), Bytes.toString(text));
		}
		return d;
	}

	@Override
	public Get get(Ref<Document> ref) {
		Assert.check(ref instanceof DocumentRef);
		return get((DocumentRef) ref);
	}

	public Get get(DocumentRef ref) {
		return new Get(Bytes.toBytes(factory_.keys().key(ref)));
	}

}