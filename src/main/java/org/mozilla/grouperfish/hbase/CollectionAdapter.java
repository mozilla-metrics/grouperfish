package org.mozilla.grouperfish.hbase;

import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.mozilla.grouperfish.base.Assert;
import org.mozilla.grouperfish.hbase.Schema.Collections.Main;
import org.mozilla.grouperfish.model.Collection;
import org.mozilla.grouperfish.model.CollectionRef;
import org.mozilla.grouperfish.model.Collection.Attribute;

class CollectionAdapter implements RowAdapter<Collection> {

  static final String DEFAULT = "DEFAULT";
  final Factory factory_;

  public
  CollectionAdapter(Factory factory) {
    factory_ = factory;
  }

  @Override public
  Put put(Collection collection) {

    final CollectionRef ref = collection.ref();
    final Put put = new Put(Bytes.toBytes(key(collection)))
    .add(Main.FAMILY, Main.NAMESPACE.qualifier, Bytes.toBytes(ref.namespace()))
    .add(Main.FAMILY, Main.KEY.qualifier, Bytes.toBytes(ref.key()));

    // Only attributes that were specified by the caller are stored.
    for (Attribute a : Collection.Attribute.values()) {
      if (collection.get(a) == null) continue;
      switch (a) {
        case MODIFIED:
          put.add(Main.FAMILY, Main.MODIFIED.qualifier,
                  Bytes.toBytes(collection.get(a)));
          break;
        case SIZE:
          put.add(Main.FAMILY, Main.SIZE.qualifier,
                  Bytes.toBytes(collection.get(a)));
          break;
        case REBUILT:
          put.add(Main.FAMILY, Main.Configuration.REBUILT.qualifier(DEFAULT),
                  Bytes.toBytes(collection.get(a)));
          break;
        case PROCESSED:
          put.add(Main.FAMILY, Main.Configuration.PROCESSED.qualifier(DEFAULT),
                  Bytes.toBytes(collection.get(a)));
          break;
        default:
          Assert.unreachable("Unknown collection attribute: ", a.name());
      }
    }
    return put;
  }

  @Override public
  String key(Collection collection) {
    return factory_.keys().key(collection.ref());
  }

  @Override public
  Collection read(Result result) {

    final Map<byte[], byte[]> main; {
      final Map<byte[], NavigableMap<byte[], byte[]>> latest =
        result.getNoVersionMap();
      main = latest.get(Main.FAMILY);
    }

    final Collection c; {
      final byte[] ns = main.get(Main.NAMESPACE.qualifier);
      final byte[] key = main.get(Main.KEY.qualifier);
      final CollectionRef ref = new CollectionRef(Bytes.toString(ns),
                                                  Bytes.toString(key));
      c = new Collection(ref);
    }

    maybeSet(c, Attribute.MODIFIED, main, Main.MODIFIED.qualifier);
    maybeSet(c, Attribute.SIZE, main, Main.SIZE.qualifier);
    maybeSet(c, Attribute.REBUILT, main,
             Main.Configuration.REBUILT.qualifier(DEFAULT));
    maybeSet(c, Attribute.PROCESSED, main,
             Main.Configuration.PROCESSED.qualifier(DEFAULT));

    return c;
  }


  private
  void maybeSet(final Collection c,
                final Attribute attr,
                final Map<byte[], byte[]> main,
                final byte[] qualifier) {
    if (!main.containsKey(qualifier)) return;
    c.set(attr, Bytes.toLong(main.get(qualifier)));
  }

}