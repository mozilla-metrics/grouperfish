package com.mozilla.grouperfish.services.hazelcast;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.mozilla.grouperfish.services.api.Grid;


public class HazelcastGrid implements Grid {

    public HazelcastGrid() {
        // Initialize Hazelcast now rather than waiting for the first request
        Hazelcast.getDefaultInstance();
        final Config config = Hazelcast.getConfig();
        for (final Map.Entry<String, MapConfig> entry : config.getMapConfigs().entrySet()) {
            Hazelcast.getMap(entry.getKey());
        }
    }

    @Override
    public Map<String, String> map(final String name) {
        return Hazelcast.getMap(name);
    }

    @Override
    public <E> BlockingQueue<E> queue(final String name) {
        return Hazelcast.getQueue(name);
    }

}
