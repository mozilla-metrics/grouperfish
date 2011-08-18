package com.mozilla.grouperfish.bootstrap;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.mozilla.bagheera.rest.Bagheera;
import com.mozilla.grouperfish.services.FileSystem;
import com.mozilla.grouperfish.services.Grid;
import com.mozilla.grouperfish.services.Index;
import com.mozilla.grouperfish.services.Services;
import com.mozilla.grouperfish.services.elasticsearch.ElasticSearchIndex;
import com.mozilla.grouperfish.services.hadoop.HadoopFileSystem;
import com.mozilla.grouperfish.services.hazelcast.HazelcastGrid;


/** Entry class to  set up the Grouperfish service. */
public class Grouperfish {

    public static final int DEFAULT_PORT = 0xF124;

    static Logger log = LoggerFactory.getLogger(Grouperfish.class);

    private static Services services;

    /**
     * Starts the Grouperfish engine.
     * REST resources will be autodiscovered by Jersey (JAX-RS).
     *
     * @param arguments not used
     * @throws Exception
     */
	public static void main(String[] arguments) throws Exception {

	    /** Poor man's IOC */
	    services = new Services() {

	        private Grid grid = new HazelcastGrid();
            private FileSystem fs = new HadoopFileSystem(new Configuration(), "/grouperfish");
            private Index index =
                new ElasticSearchIndex(System.getProperty("grouperfish.elasticsearch.cluster", "grouperfish"));

            @Override
            public Grid grid() { return grid; }

            @Override
            public Index index() { return index; }

            @Override
            public FileSystem fs() { return fs; }

        };

	    Bagheera.main(arguments);

	}

	public static Services services() {
	    return services;
	}

}
