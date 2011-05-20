package org.mozilla.grouperfish.model;

public class Document implements Model {

  public
  Document(DocumentRef ref, String text) {
    ref_ = ref;
    text_ = text;
  }

  @Override public
  DocumentRef ref() { return ref_; }


  public
  String text() { return text_; }


  private final DocumentRef ref_;
  private final String text_;

}
