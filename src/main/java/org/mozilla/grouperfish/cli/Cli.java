package org.mozilla.grouperfish.cli;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.mozilla.grouperfish.base.Assert;
import org.mozilla.grouperfish.conf.Conf;
import org.mozilla.grouperfish.input.StreamImporter;
import org.mozilla.grouperfish.jobs.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Cli {

  public static final Logger log = LoggerFactory.getLogger(Cli.class);

  static private final String USAGE =
    "Usage: java -jar grouperfish.jar [--config PATH] \\\n" +
    "              [import <ns> | collection-info <ns> <ck> | help]\n" +
    " import    read (opinion) data from stdin\n" +
    "   list    prints all documents of the given collection\n" +
    "  job:*    start a hadoop job, such as a full cluster rebuild \n" +
    "   help    print this message and exit";


  public
  Cli(Conf conf) {
    conf_ = conf;
  }


  static private
  void exit(String message, int status) {
    (status == 0 ? System.out : System.err).println(message);
    System.exit(status);
  }


  static private
  void exit(int status) {
    System.exit(status);
  }


  public
  void collectionInfo(String namespace, String collectionKey) {
    Assert.unreachable("Implementz me!");
  }


  public
  int load(String namespace, InputStream in) {
    final String CONF_KEY_PATTERNS = "input:import:collections:patterns";
    List<String> patterns = conf_.getList(String.class, CONF_KEY_PATTERNS);
    int[] counters =
      new StreamImporter(conf_, namespace, patterns).load(in);

    log.info("Counters: #discarded/#used/#docs/#collections: {}",
             Arrays.toString(counters));
    return 0;
  }


  static public
  void main(final String[] args) {
    List<String> arguments = Arrays.asList(args);
    String configPath = null;
    int i = 0;
    {
      while (arguments.size() > i && arguments.get(i).startsWith("--")) {
        if ("--help".equals(arguments.get(i))) {
          exit(USAGE, 0);
        }
        if ("--config".equals(arguments.get(i))) {
          configPath = arguments.get(++i);
        }
        else {
          exit(USAGE, 1);
        }
        ++i;
      }
      if (arguments.size() == i) exit(USAGE, 1);
    }
    final Conf conf = (new org.mozilla.grouperfish.conf.Factory()).conf(configPath);

    final String command = arguments.get(i);
    ++i;
    final List<String> cmdArgs = arguments.subList(i, arguments.size());

    if ("help".equals(command)) {
      exit(USAGE, 0);
    }

    if ("import".equals(command) && cmdArgs.size() == 1) {
      new Cli(conf).load(cmdArgs.get(0), System.in);
    }
    else if ("list".equals(command) && cmdArgs.size() >= 2) {
      new Cli(conf).collectionInfo(cmdArgs.get(0), cmdArgs.get(1));
    }
    else if (command.startsWith("job:")) {
      exit(new Util(conf).run(command.substring(4),
                              cmdArgs.toArray(new String[]{})));
    }
    else {
      exit(USAGE, 1);
    }
  }


  private final Conf conf_;

}
