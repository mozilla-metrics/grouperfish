package com.mozilla.grouperfish.batch.api.guice;

import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.mozilla.grouperfish.batch.api.BatchService;
import com.mozilla.grouperfish.batch.scheduling.SynchronousBatchService;
import com.mozilla.grouperfish.batch.transforms.HadoopTransform;
import com.mozilla.grouperfish.batch.transforms.LocalTransform;
import com.mozilla.grouperfish.batch.transforms.Transform;
import com.mozilla.grouperfish.batch.transforms.TransformProvider;
import com.mozilla.grouperfish.services.api.FileSystem;
import com.mozilla.grouperfish.services.api.guice.Local;
import com.mozilla.grouperfish.services.api.guice.Services;
import com.mozilla.grouperfish.services.api.guice.Shared;

public class BatchSystem extends AbstractModule {

    @Override
    protected void configure() {
        bind(BatchService.class).to(SynchronousBatchService.class).asEagerSingleton();
        bind(TransformProvider.class).to(StaticTransformProvider.class).asEagerSingleton();
    }

    static class StaticTransformProvider implements TransformProvider {

        private final ImmutableMap<String, Transform> transformsByName;

        @Inject
        public StaticTransformProvider(
                final Properties properties,
                final @Shared FileSystem dfs,
                final @Local FileSystem localFs) {

            final ImmutableMap.Builder<String, Transform> builder =
                new ImmutableMap.Builder<String, Transform>();

            builder.put("count", new LocalTransform("count", dfs, localFs));
            builder.put("textcluster", new LocalTransform("textcluster", dfs, localFs));
            // :TODO: Next: autodiscover available transforms

            if (Services.hasHadoop(properties)) {
                builder.put("coclustering", new HadoopTransform("coclustering", dfs));
            }

            transformsByName = builder.build();
        }

        @Override
        public Transform get(final String name) {
            return transformsByName.get(name);
        }

    }

}
