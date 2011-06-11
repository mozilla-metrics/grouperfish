package com.mozilla.grouperfish.hbase;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.hbase.Schema.Documents;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.DocumentRef;
import com.mozilla.grouperfish.model.Ref;

class DocumentAdapter implements RowAdapter<Document> {

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

	@Override
	public Document read(Result next) {
		return Assert.unreachable(Document.class, "Not implemented.");
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