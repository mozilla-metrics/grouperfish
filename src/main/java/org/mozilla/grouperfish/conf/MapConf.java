package org.mozilla.grouperfish.conf;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import edu.emory.mathcs.backport.java.util.Collections;


class MapConf implements Conf {

  MapConf(Map<String, ?> map) {
    map_ = map;
  }


  @Override public
  String toJSON() {
    if (json_ == null) json_ = JSONObject.toJSONString(map_);
    return json_;
  }


  @Override public
  String get(String key) {
    return (String) map_.get(key);
  }


  @SuppressWarnings("unchecked") @Override public <C>
  List<C> getList(Class<C> valueType, String key) {
    // If the value is not a List, fail here rather than later.
    return (List<C>) map_.get(key);
  }

  @SuppressWarnings("unchecked") @Override public
  Map<String, ?> asMap() {
    return Collections.unmodifiableMap(map_);
  }


  private final Map<String, ?> map_;
  private String json_;

}
