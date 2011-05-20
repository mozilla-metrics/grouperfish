package org.mozilla.grouperfish.model;

import java.util.Map;


/**
 * Most models require all sorts of fields being set.
 *
 * With collections, the opposite is the case, because we usually want to get/update very specific
 * datums. Updates to other columns make no sense and might even undo other valid updates.
 * When put to the db, only the given columns will be overwritten. Set an attributes to
 * <tt>null</tt> to prevent it from being written to the database.
 */
public class Collection implements Model {

  @Override public
  CollectionRef ref() { return ref_; }

  public
  Long get(Attribute attr) { return attributes_.get(attr); }


  public
  Collection set(Attribute attr, Long value) {
    attributes_.put(attr, value);
    return this;
  }


  public static enum Attribute {
    /** The size of the collection. */
    SIZE,
    /** When the last document was added to the collection. */
    MODIFIED,
    /** When the last collection was (incrementally or fully) processed last. */
    PROCESSED,
    /** When the last full rebuild was started. */
    REBUILT
  }


  public
  Collection(CollectionRef ref) {
    // Configuration independent:
    ref_ = ref;
    attributes_ = new java.util.HashMap<Attribute, Long>();
  }


  private final Map<Attribute, Long> attributes_;

  private final CollectionRef ref_;

}
