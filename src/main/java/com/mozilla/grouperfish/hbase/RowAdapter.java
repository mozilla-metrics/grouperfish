package com.mozilla.grouperfish.hbase;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import com.mozilla.grouperfish.model.Model;
import com.mozilla.grouperfish.model.Ref;

/** Handles HBase CRUD for individual model objects. */
public interface RowAdapter<S extends Model> {

	Get get(Ref<S> ref);

	Put put(S item);

	String key(S item);

	S read(Result next);

}
