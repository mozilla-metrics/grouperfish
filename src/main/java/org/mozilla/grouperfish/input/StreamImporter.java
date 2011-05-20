package org.mozilla.grouperfish.input;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.grouperfish.conf.Conf;
import org.mozilla.grouperfish.hbase.Factory;
import org.mozilla.grouperfish.input.Opinions.Field;
import org.mozilla.grouperfish.model.Collection;
import org.mozilla.grouperfish.model.CollectionRef;
import org.mozilla.grouperfish.model.Document;
import org.mozilla.grouperfish.model.DocumentRef;
import org.mozilla.grouperfish.model.Collection.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Loads opinions from a stream into all applicable collections.
 * Also creates these collections.
 */
public class StreamImporter {

  public
  StreamImporter(final Conf conf,
                 final String namespace,
                 final List<String> keyPatterns) {
    factory_ = new Factory(conf);
    namespace_ = namespace;
    keyGens_ = new KeyGen[keyPatterns.size()];
    int i = 0;
    for (String pattern : keyPatterns) {
      keyGens_[i++] = new KeyGen(pattern);
    }
  }


  private static
  enum Counters {
    ROWS_USED,
    ROWS_DISCARDED,
    DOCS_CREATED,
    COLLECTIONS_CREATED
  };


  public int[]
  load(final InputStream in) {
    final Map<String, Integer> sizes = new HashMap<String, Integer>();
    final Map<String, CollectionRef> collectionRefs =
      new HashMap<String, CollectionRef>();

    DocumentsGenerator gen = new DocumentsGenerator(in, collectionRefs, sizes);
    factory_.importer(Document.class).load(gen);
    log.info("All documents loaded.");

    int[] counters = gen.counters;
    List<Collection> collections = new java.util.ArrayList<Collection>();
    for (CollectionRef ref : collectionRefs.values()) {
      counters[Counters.COLLECTIONS_CREATED.ordinal()]++;
      Collection c = new Collection(ref);
      c.set(Attribute.SIZE, sizes.get(ref.key()).longValue());
      c.set(Attribute.MODIFIED, new Date().getTime());
      collections.add(c);
    }

    factory_.importer(Collection.class).load(collections);
    log.info("All collections loaded.");
    return counters;
  }


  /**
   * Generates documents for all collections applicable to an input opinion.
   * Also maintains the list of collections (in place) so we import those as
   * well.
   */
  class DocumentsGenerator implements Iterable<Document> {
    final int[] counters = new int[Counters.values().length];
    final InputStream in_;
    final Map<String, CollectionRef> collectionRefs_;
    final Map<String, Integer> sizes_;
    DocumentsGenerator(final InputStream in,
                       final Map<String, CollectionRef> collectionRefs,
                       final Map<String, Integer> sizes) {
      in_ = in;
      collectionRefs_ = collectionRefs;
      sizes_ = sizes;
    }

    @Override public
    Iterator<Document> iterator() {
      return new Iterator<Document>() {

        final Iterator<String[]> it_ = new Opinions(in_).iterator();
        String[] opinion_ = it_.hasNext() ? it_.next() : null;
        int k_ = 0;

        @Override
        public Document next() {
          if (!hasNext()) throw new NoSuchElementException();
          final String key = keyGens_[k_].key(opinion_);

          Integer size = sizes_.get(key);
          if (size == null) {
            collectionRefs_.put(key, new CollectionRef(namespace_, key));
            size = Integer.valueOf(0);
          }
          sizes_.put(key, Integer.valueOf(size.intValue()+1));

          final DocumentRef ref =
            new DocumentRef(collectionRefs_.get(key), opinion_[Field.ID.i]);
          final Document doc = new Document(ref, opinion_[Field.TEXT.i]);
          counters[Counters.DOCS_CREATED.ordinal()]++;

          k_ = (k_+1) % keyGens_.length;
          if (k_ == 0) opinion_ = null;
          return doc;
        }

        @Override
        public boolean hasNext() {
          if (opinion_ != null) return true;
          if (!it_.hasNext()) return false;
          do opinion_ = it_.next(); while (it_.hasNext() && !isOk(opinion_));
          return opinion_ != null;
        }

        @Override
        public void remove() { throw new UnsupportedOperationException(); }

        private boolean isOk(String[] opinion) {
          if (opinion == null || opinion[Field.TEXT.i].length() == 0) {
            counters[Counters.ROWS_DISCARDED.ordinal()]++;
            return false;
          }
          counters[Counters.ROWS_USED.ordinal()]++;
          return true;
        }
      };
    }
  }


  /**
   * Generates collection keys from patterns. Patterns are strings that use
   * field names like variables. Examples:
   * Note that there is no escape syntax.
   * "$PRODUCT:$VERSION-$PLATFORM-$TYPE" will produce keys such as
   * "firefox:4.0b12-mac-issue".
   */
  private static class KeyGen {

    final String[] fragments_;
    final Field[] fields_;

    KeyGen (String pattern) {
      // Contains a null where a fragment should be inserted.
      List<Field> fields = new ArrayList<Opinions.Field>();
      List<String> fragments = new ArrayList<String>();
      Matcher matcher = Pattern.compile("[$]([a-zA-Z0-9]+)").matcher(pattern);
      while (matcher.find()) {
        StringBuffer tmp = new StringBuffer();
        matcher.appendReplacement(tmp, "");
        if (tmp.length() > 0) {
          fields.add(null);
          fragments.add(tmp.toString());
        }
        fields.add(Field.valueOf(matcher.group().substring(1)));
      }
      fragments_ = fragments.toArray(new String[fragments.size()]);
      fields_ = fields.toArray(new Field[fields.size()]);
    }

    String key(final String[] opinion) {
      final StringBuilder sb = new StringBuilder(24);
      int frag = 0;
      for (Field field : fields_) {
        if (field == null) sb.append(fragments_[frag++]);
        else sb.append(opinion[field.ordinal()]);
      }
      return sb.toString();
    }

  }


  private static final Logger log =
    LoggerFactory.getLogger(StreamImporter.class);

  private final String namespace_;
  private final KeyGen[] keyGens_;
  private final Factory factory_;

}
