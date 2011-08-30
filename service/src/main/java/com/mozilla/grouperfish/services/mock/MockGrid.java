package com.mozilla.grouperfish.services.mock;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.mozilla.grouperfish.services.api.Grid;


/**
 * In memory grid service, usable for some mocking.
 *
 * This cannot be used as an actual replacement for Hazelcast
 * because it lacks the persistence/indexing provided by Bagheera.
 *
 * Make sure to instantiate this as a singleton (e.g. using Guice).
 */
public class MockGrid implements Grid {

    // We want a concurrent map, like Hazelcast provides.
    private final Map<String, Map<String, String>> maps =
        new Hashtable<String, Map<String, String>>();

    private final int queueCapacity = 1000;

    private final Map<String, BlockingQueue<?>> queues =
        new Hashtable<String, BlockingQueue<?>>();

    @Override
    public synchronized Map<String, String> map(final String name) {
        if (!maps.containsKey(name)) {
            maps.put(name, new Hashtable<String, String>());
        }
        return maps.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <E> BlockingQueue<E> queue(final String name) {

        if (!queues.containsKey(name)) {
            queues.put(name, new ArrayBlockingQueue<E>(queueCapacity));
        }

        return (BlockingQueue<E>) queues.get(name);
    }

}
