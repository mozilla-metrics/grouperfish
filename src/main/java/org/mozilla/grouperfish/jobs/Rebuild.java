package org.mozilla.grouperfish.jobs;

import org.apache.hadoop.conf.Configuration;
import org.mozilla.grouperfish.conf.Conf;
import org.mozilla.grouperfish.jobs.textcluster.TextClusterTool;
import org.mozilla.grouperfish.model.CollectionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Completely rebuilds a collection using the appropriate algorithm for every
 * configured clustering configuration.
 */
public class Rebuild extends AbstractCollectionTool {

  public Rebuild(Conf conf, Configuration hadoopConf) {
    super(conf, hadoopConf);
  }


  @Override public
  int run(CollectionRef collection, long timestamp) throws Exception {
    final CollectionTool[] toolchain = new CollectionTool[]{
        new ExportDocuments(conf_, getConf()),
        new VectorizeDocuments(conf_, getConf()),
        new TextClusterTool(conf_, getConf())
    };
    for (final CollectionTool tool : toolchain) {
      int returnCode = tool.run(collection, timestamp);
      if (returnCode != 0) {
        log.error("Error running job {}", tool.name());
        return returnCode;
      }
    }
    return 0;
  }


  @Override public
  String name() {
    return NAME;
  }

  private static final Logger log = LoggerFactory.getLogger(Rebuild.class);

  static String NAME = "rebuild";

}
