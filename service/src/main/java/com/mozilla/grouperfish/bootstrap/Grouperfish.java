package com.mozilla.grouperfish.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.mozilla.grouperfish.batch.api.guice.BatchSystem;
import com.mozilla.grouperfish.rest.api.RestService;
import com.mozilla.grouperfish.rest.jersey.JerseyGuiceRestService;
import com.mozilla.grouperfish.rest.jersey.ResourceConfig;
import com.mozilla.grouperfish.services.api.guice.Services;

/** Entry class to  set up the Grouperfish service. */
public class Grouperfish {

    public static final int DEFAULT_PORT = 0xF124;

    static Logger log = LoggerFactory.getLogger(Grouperfish.class);

    /**
     * Starts the Grouperfish engine.
     * REST resources will be autodiscovered by Jersey (JAX-RS).
     *
     * @param arguments not used
     * @throws Exception
     */
	public static void main(final String[] arguments) throws Exception {
	    new Grouperfish(new Services(), new BatchSystem());
	}

	public Grouperfish(final Module... modules) {
	    final Injector injector = Guice.createInjector(modules);
	    final RestService rest = new JerseyGuiceRestService(injector, ResourceConfig.class);
	    rest.start();
	}
}
