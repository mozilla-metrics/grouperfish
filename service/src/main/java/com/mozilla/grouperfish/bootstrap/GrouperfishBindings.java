package com.mozilla.grouperfish.bootstrap;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.mozilla.grouperfish.batch.BatchService;
import com.mozilla.grouperfish.batch.PipeliningBatchService;
import com.mozilla.grouperfish.batch.run.DistributedTransform;
import com.mozilla.grouperfish.batch.run.LocalTransform;
import com.mozilla.grouperfish.batch.run.Transform;
import com.mozilla.grouperfish.batch.run.TransformProvider;
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
        bind(TransformProvider.class).to(StaticTransformProvider.class).asEagerSingleton();
        bind(BatchService.class).to(PipeliningBatchService.class).asEagerSingleton();
    }

    static class StaticTransformProvider implements TransformProvider {

        private final ImmutableMap<String, Transform> transformsByName;

        @Inject
        public StaticTransformProvider(
                final FileSystem dfs,
                final FileSystem localFs) {
            final ImmutableMap.Builder<String, Transform> builder =
                new ImmutableMap.Builder<String, Transform>();

            // :TODO: Next: autodiscover available transforms
            builder.put("count", new LocalTransform("count", dfs, localFs));
            builder.put("coclustering", new DistributedTransform("coclustering", dfs));
            transformsByName = builder.build();
        }

        @Override
        public Transform get(final String name) {
            return transformsByName.get(name);
        }

    }

}
