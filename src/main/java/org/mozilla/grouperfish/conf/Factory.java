package org.mozilla.grouperfish.conf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mozilla.grouperfish.base.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Factory {

  private static final Logger log =
    LoggerFactory.getLogger(Factory.class);


  public
  Factory() { }


  public
  Conf conf(final String optionalFilePath) {

    String home = null;
    try {
      home = System.getenv("GROUPERFISH_HOME");
    }
    catch (NullPointerException npe) {
      /* javadoc says getenv throws npe on missing key, but does not... */
    }
    if (home == null) {
      home = new File(path("../../")).getAbsolutePath();
    }

    final String defaultPath = path(home, "conf/defaults.json");
    final Conf defaultConf = fromFile(defaultPath);

    final String filePath = optionalFilePath != null ?
        optionalFilePath :
        path(home, "conf/grouperfish.json");
    final Conf userConf = fromFile(filePath);

    if (defaultConf == null) {
      log.warn("Looking for defaults in {}, for user configuration.in {}",
               defaultPath, filePath);
      if (userConf != null) return userConf;
      log.error("Could find neither user-provided configuration nor defaults!");
      Assert.unreachable("Cannot proceed without Grouperfish configuration.");
    }
    if (userConf == null) return defaultConf;
    return compose(defaultConf, userConf);
  }

  /**
   * @param path A relative path to the configuration.
   * @return The configuration for this file. Null if the file does not exist.
   */
  @SuppressWarnings("unchecked") public
  Conf fromFile(final String path) {
    if (!new File(path).exists()) return null;
    try {
      final Reader source = new BufferedReader(new FileReader(path));
      try {
        final Map<String, ?> map;
        map = (Map<String, Object>) new JSONParser().parse(source, CONTAINERS);
        return new MapConf(map);
      }
      finally {
        source.close();
      }
    }
    catch (Exception error) {
      throw new RuntimeException(error);
    }
  }


  @SuppressWarnings("unchecked") public
  Conf fromJSON(final String json) {
    final JSONParser parser = new JSONParser();
    final Map<String, ?> map;
    try {
      map = (Map<String, ? extends Object>) parser.parse(json, CONTAINERS);
      return new MapConf(map);
    }
    catch (ParseException e) {
      log.error("Error parsing JSON configuration:");
      e.printStackTrace();
      return Assert.unreachable(Conf.class);
    }
  }


  public
  Conf fromMap(Map<String, ?> map) {
    return new MapConf(map);
  }


  /** Returns a composite conf, right values override left values. */
  public
  Conf compose(Conf bottom, Conf... more) {
    Conf top = new CompositeConf(bottom);
    for (Conf next : more) top = new CompositeConf(next, top);
    return top;
  }


  @SuppressWarnings("rawtypes") final static private
  ContainerFactory CONTAINERS = new ContainerFactory() {
    @Override public
    List creatArrayContainer() { return new java.util.ArrayList(); }
    @Override public
    Map createObjectContainer() { return new java.util.HashMap(); }
  };


  private
  String path(String... path) {
    if (File.separatorChar != '/') {
      final StringBuffer res = new StringBuffer();
      for (String el : path) res.append(el.replace('/', File.separatorChar));
      return res.toString();
    }
    return StringUtils.join(path, File.separatorChar);
  }
}
