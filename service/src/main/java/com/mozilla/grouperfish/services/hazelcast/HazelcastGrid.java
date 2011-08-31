package com.mozilla.grouperfish.services.hazelcast;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.mozilla.grouperfish.services.api.Grid;


public class HazelcastGrid implements Grid {

    private static final Logger log = LoggerFactory.getLogger(HazelcastGrid.class);

    public HazelcastGrid() {
        // Initialize Hazelcast now rather than waiting for the first request
        Hazelcast.getDefaultInstance();
        final Config config = Hazelcast.getConfig();
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, MapConfig> entry : config.getMapConfigs().entrySet()) {
            Hazelcast.getMap(entry.getKey());
            sb.append(entry.getKey());
        }
        final int numMembers = Hazelcast.getCluster().getMembers().size();
        log.info(String.format("Instantiated service: %s (maps=%s, #members=%s)",
            getClass().getSimpleName(), sb.toString(), numMembers));
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
