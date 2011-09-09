    package com.mozilla.grouperfish.services.api.guice;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Grid;
import com.mozilla.grouperfish.services.api.IndexProvider;
import com.mozilla.grouperfish.services.elasticsearch.ElasticSearchIndexProvider;
import com.mozilla.grouperfish.services.hadoop.HadoopFileSystem;
import com.mozilla.grouperfish.services.hazelcast.HazelcastGrid;
import com.mozilla.grouperfish.services.local.LocalFileSystem;


/** Grouperfish default service bindings. */
public class Services extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Services.class);

    /** One of "standalone" (default) or "cluster". */
    public static final String PROPERTY_MODE = "grouperfish.services.mode";
    public static final String PROPERTY_MODE_DEFAULT = "standalone";

    /** Either "true" (use HDFS and allow for hadoop based transforms), or "false" (default). */
    public static final String PROPERTY_HADOOP_ENABLED = "grouperfish.services.hadoop.enabled";
    public static final String PROPERTY_HADOOP_ENABLED_DEFAULT = "false";

    private final Properties properties;

    public Services(final Properties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure() {

        final FileSystem localFs = new LocalFileSystem("./data/local/");

        bind(FileSystem.class).annotatedWith(Local.class).toInstance(localFs);
        bind(FileSystem.class).toInstance(localFs);
        if (hasHadoop(properties)) {
            log.info("Hadoop available. Using HDFS for shared file system.");
            bind(FileSystem.class).annotatedWith(Shared.class).to(HadoopFileSystem.class).asEagerSingleton();
        }
        else {
            if (!isStandalone(properties)) {
                // Currently hadoop is our only shared FS provider.
                throw new IllegalStateException(String.format(
                        "Grouperfish is configured for clustered operation (by %s), but hadoop is disabled (by %s)!",
                        PROPERTY_MODE,
                        PROPERTY_HADOOP_ENABLED));
            }
            log.info("No hadoop. Using local FS for 'shared' file system (stand alone operation).");
            bind(FileSystem.class).annotatedWith(Shared.class).toInstance(localFs);
        }

        bind(Grid.class).to(HazelcastGrid.class).asEagerSingleton();
        bind(IndexProvider.class).toInstance(new ElasticSearchIndexProvider(properties));
    }


    /**
     * This is true if (and only if) hadoop is available.
     * If false, transforms that require hadoop will not be available.
     */
    public static boolean hasHadoop(final Properties properties) {
        final String hadoopEnabled = properties.getProperty(PROPERTY_HADOOP_ENABLED, PROPERTY_HADOOP_ENABLED_DEFAULT);
        return "true".equals(hadoopEnabled);
    }


    /**
     * This is true if (and only if) we are the only Grouperfish node in the swarm.
     * Does not prevent from using hadoop.
     */
    public static boolean isStandalone(final Properties properties) {
        final String mode = properties.getProperty(PROPERTY_MODE, PROPERTY_MODE_DEFAULT);
        return "standalone".equals(mode);
    }

}
