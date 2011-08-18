package com.mozilla.grouperfish.services.hazelcast;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.hazelcast.core.Hazelcast;
import com.mozilla.grouperfish.services.Grid;

public class HazelcastGrid implements Grid {

    @Override
    public Map<String, String> map(final String name) {
        return Hazelcast.getMap(name);
    }

    @Override
    public <E> BlockingQueue<E> queue(final String name) {
        return Hazelcast.getQueue(name);
    }

}
