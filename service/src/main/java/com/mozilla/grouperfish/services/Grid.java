package com.mozilla.grouperfish.services;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public interface Grid {

    Map<String, String> map(String name);

    <E> BlockingQueue<E> queue(String name);

}
