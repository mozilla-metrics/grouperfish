package com.mozilla.grouperfish.services.api;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public interface Grid {

    Map<String, String> map(String name);

    <E> BlockingQueue<E> queue(String name);

}
