package com.mozilla.grouperfish.rest.jersey;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.bootstrap.Grouperfish;
import com.mozilla.grouperfish.rest.api.RestService;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;


public class JerseyGuiceRestService implements RestService {

    public static final String PROPERTY_PORT = "grouperfish.rest.port";

    private final Server server;

    /**
     * Initializes a Jersey based JAX-RS service using the given resource configuration.
     * The procided confgiuration class must not be anonymous.
     */
    public JerseyGuiceRestService(final Injector parentInjector,
                      final Class<? extends ResourceConfig> resourceConfigClass) {

        Assert.nonNull(parentInjector);
        Assert.nonNull(resourceConfigClass, resourceConfigClass.getCanonicalName());
        Assert.check(!resourceConfigClass.getCanonicalName().isEmpty());

        final int port = Integer.parseInt(System.getProperty(PROPERTY_PORT, String.valueOf(Grouperfish.DEFAULT_PORT)));

        server = new Server(port);
        final ServletContextHandler root =
            new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS);

        root.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return parentInjector.createChildInjector(new JerseyServletModule() {
                    protected void configureServlets() {
                        final Map<String, String> params = new HashMap<String, String>();
                        params.put(PackagesResourceConfig.PROPERTY_PACKAGES,
                                   "jetty");
                        params.put("com.sun.jersey.config.property.resourceConfigClass",
                                   resourceConfigClass.getCanonicalName());
                        serve("/*").with(GuiceContainer.class, params);
                    }
                });
            }
        });

        root.addFilter(GuiceFilter.class, "/*", null);

        // Must add DefaultServlet for embedded Jetty.
        // Failing to do this will cause 404 errors.
        // This is not needed if web.xml is used instead.
        root.addServlet(DefaultServlet.class, "/");

        server.setSendServerVersion(false);
        server.setSendDateHeader(false);
        server.setStopAtShutdown(true);

    }

    @Override
    public Daemon start() {
        try {
            server.start();
        }
        catch(final Exception e) {
            throw new RuntimeException(e);
        }

        return new Daemon() {
            @Override
            public void join() throws InterruptedException {
                server.join();
            }
        };
    }

}
