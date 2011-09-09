package com.mozilla.grouperfish.bootstrap;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.mozilla.grouperfish.base.PropertiesTool;
import com.mozilla.grouperfish.batch.api.guice.BatchSystem;
import com.mozilla.grouperfish.rest.api.RestService;
import com.mozilla.grouperfish.rest.jersey.JerseyGuiceRestService;
import com.mozilla.grouperfish.rest.jersey.ResourceConfig;
import com.mozilla.grouperfish.services.api.guice.Services;


/** Entry class to  set up the Grouperfish service. */
public class Grouperfish {

    public static final int DEFAULT_PORT = 0xF124;

    static final Logger log = LoggerFactory.getLogger(Grouperfish.class);

    /**
     * Starts the Grouperfish engine.
     * REST resources will be autodiscovered by Jersey (JAX-RS).
     *
     * @param arguments not used
     * @throws Exception
     */
	public static void main(final String[] arguments) throws Exception {
	    final Properties properties =
            PropertiesTool.load(Grouperfish.class, "grouperfish.properties");
	    new Grouperfish(
	            new Services(properties),
	            new BatchSystem(),
	            new AbstractModule() {
	                @Override protected void configure() {
	                    bind(Properties.class).toProvider(new Provider<Properties>() {
	                        @Override public Properties get() { return properties; }
	                    }).asEagerSingleton();
	                }
	            }
	    );
	}

	public Grouperfish(final Module... modules) {
        SLF4JBridgeHandler.install();
	    final Injector injector = Guice.createInjector(modules);
	    final RestService rest = new JerseyGuiceRestService(injector, ResourceConfig.class);
	    rest.start();
	    log.info("Grouperfish started.");
        log.debug("Configured port: {}, default: {}",
                  System.getProperty(JerseyGuiceRestService.PROPERTY_PORT), DEFAULT_PORT);
	}

}
