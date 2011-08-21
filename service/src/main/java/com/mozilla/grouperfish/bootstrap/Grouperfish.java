package com.mozilla.grouperfish.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mozilla.grouperfish.services.jersey.JerseyStorageAndRetrieval;
import com.mozilla.grouperfish.services.jersey.ResourceConfig;

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
	    new Grouperfish(new GrouperfishBindings());
	}

	public Grouperfish(final AbstractModule bindings) {
	    final Injector injector = Guice.createInjector(bindings);
	    new JerseyStorageAndRetrieval(injector, ResourceConfig.class);
	}
}
