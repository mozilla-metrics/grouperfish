package com.mozilla.grouperfish.services.elasticsearch;

import java.util.Properties;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.services.api.Index;
import com.mozilla.grouperfish.services.api.IndexProvider;

public class ElasticSearchIndexProvider implements IndexProvider {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndexProvider.class);

    public static final String PROPERTY_CLUSTER = "grouperfish.services.elasticsearch.cluster";
    public static final String PROPERTY_CLUSTER_DEFAULT = "grouperfish";

    public static final String PROPERTY_TYPE = "grouperfish.services.elasticsearch.type";
    // :TODO: Hack... to simplify, we should use 1 index for all HC maps, and differentiate solely using type.
    public static final String PROPERTY_TYPE_DEFAULT = "documents";

    private final String type;
    private final Client client;

    public ElasticSearchIndexProvider(final Properties properties) {
        type = System.getProperty(PROPERTY_TYPE, PROPERTY_TYPE_DEFAULT);
        final String clusterName = System.getProperty(PROPERTY_CLUSTER, PROPERTY_CLUSTER_DEFAULT);
        final Node node = NodeBuilder.nodeBuilder().loadConfigSettings(false).client(true).data(false).clusterName(clusterName).build();
        node.start();
        client = node.client();

        log.info(String.format("Instantiated index provider: %s (cluster.name=%s)",
                               getClass().getSimpleName(), clusterName));
    }

    @Override
    public Index index(final String name) {
        return new ElasticSearchIndex(client, name, type);
    }

}
