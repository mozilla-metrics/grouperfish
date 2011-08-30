    package com.mozilla.grouperfish.services.api.guice;

import com.google.inject.AbstractModule;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.Grid;
import com.mozilla.grouperfish.services.api.Index;
import com.mozilla.grouperfish.services.elasticsearch.ElasticSearchIndex;
import com.mozilla.grouperfish.services.hadoop.HadoopFileSystem;
import com.mozilla.grouperfish.services.hazelcast.HazelcastGrid;
import com.mozilla.grouperfish.services.local.LocalFileSystem;


/** Grouperfish default service bindings. */
public class Services extends AbstractModule {

    public static final String PROPERTY_MODE = "grouperfish.services.mode";
    public static final String PROPERTY_HADOOP_ENABLED = "grouperfish.services.hadoop.enabled";

    @Override
    protected void configure() {

        final FileSystem localFs = new LocalFileSystem("./data/local/");

        bind(FileSystem.class).annotatedWith(Local.class).toInstance(localFs);
        if (hasHadoop()) {
            bind(FileSystem.class).annotatedWith(Shared.class).to(HadoopFileSystem.class).asEagerSingleton();
        }
        else {
            if (!isStandalone()) {
                // Currently hadoop is our only shared FS provider.
                throw new IllegalStateException(String.format(
                        "Grouperfish is configured for clustered operation (by %s), but hadoop is disabled (by %s)!",
                        PROPERTY_MODE,
                        PROPERTY_HADOOP_ENABLED));
            }
            bind(FileSystem.class).annotatedWith(Shared.class).toInstance(localFs);
        }

        bind(Grid.class).to(HazelcastGrid.class).asEagerSingleton();
        bind(Index.class).to(ElasticSearchIndex.class).asEagerSingleton();
        bind(FileSystem.class).to(HadoopFileSystem.class).asEagerSingleton();
    }


    /**
     * This is true if (and only if) hadoop is available.
     * If false, transforms that require hadoop will not be available.
     */
    public static boolean hasHadoop() {
        final String hadoopEnabled = System.getProperty(PROPERTY_HADOOP_ENABLED, String.valueOf("true"));
        return "true".equals(hadoopEnabled);
    }


    /**
     * This is true if (and only if) we are the only Grouperfish node in the swarm.
     * Does not prevent from using hadoop.
     */
    public static boolean isStandalone() {
        final String mode = System.getProperty(PROPERTY_MODE, String.valueOf("cluster"));
        return "standalone".equals(mode);
    }

}
