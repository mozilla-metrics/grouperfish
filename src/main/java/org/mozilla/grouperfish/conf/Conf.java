package org.mozilla.grouperfish.conf;

import java.util.List;
import java.util.Map;

public interface Conf {

  String toJSON();

  String get(String key);

  <C> List<C> getList(Class<C> valueType, String key);

  Map<? extends String, ?> asMap();

}
