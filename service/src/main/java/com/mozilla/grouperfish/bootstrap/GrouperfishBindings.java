package com.mozilla.grouperfish.bootstrap;

import com.google.inject.AbstractModule;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.Grid;
import com.mozilla.grouperfish.services.Index;
import com.mozilla.grouperfish.services.elasticsearch.ElasticSearchIndex;
import com.mozilla.grouperfish.services.hadoop.HadoopFileSystem;
import com.mozilla.grouperfish.services.hazelcast.HazelcastGrid;


/** Guice service bindings that grouperfish uses by default. */
public class GrouperfishBindings extends AbstractModule {

    @Override
    protected void configure() {
        bind(Grid.class).to(HazelcastGrid.class).asEagerSingleton();
        bind(Index.class).to(ElasticSearchIndex.class).asEagerSingleton();
        bind(FileSystem.class).to(HadoopFileSystem.class).asEagerSingleton();
    }

}
