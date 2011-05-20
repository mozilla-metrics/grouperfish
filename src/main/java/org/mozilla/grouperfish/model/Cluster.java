package org.mozilla.grouperfish.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;


public class Cluster extends BaseCluster implements Model {

  public
  Cluster(ClusterRef ref,
          Vector medoid,
          List<Vector> related,
          List<Double> similarities) {
    super(medoid, related, similarities);
    ref_ = ref;
  }


  public
  Cluster(ClusterRef ref, BaseCluster data) {
    super(data);
    ref_ = ref;
  }


  public
  DocumentRef representativeDoc() {
    return new DocumentRef(ref_.ownerRef(), ((NamedVector) medoid()).getName());
  }


  public
  List<DocumentRef> relatedDocs() {
    if (!contents_.isEmpty()) return contents_;
    final CollectionRef owner_ = ref_.ownerRef();
    for (Vector v : related()) {
      contents_.add(new DocumentRef(owner_, ((NamedVector) v).getName()));
    }
    return contents_;
  }


  @Override public
  ClusterRef ref() { return ref_; }

  private final ClusterRef ref_;
  private final List<DocumentRef> contents_ = new ArrayList<DocumentRef>();

}
