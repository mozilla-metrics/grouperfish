package com.mozilla.grouperfish.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.sun.jersey.spi.container.servlet.ServletContainer;


/** Entry class to  set up the Grouperfish service. */
public class Grouperfish {
    
    static Logger log = LoggerFactory.getLogger(Grouperfish.class);

    /**
     * 
     * @param args
     * @throws Exception 
     */
	public static void main(String[] args) throws Exception {
	    
	    // Initialize HazelCast
	    
	    Hazelcast.getDefaultInstance();
        Config config = Hazelcast.getConfig();
        log.debug("Checking map: {}", "documents_test");
        Hazelcast.getMap("documents_test");
        for (Map.Entry<String, MapConfig> entry : config.getMapConfigs().entrySet()) {
            String mapName = entry.getKey() + "test";
            log.debug("Checking map: {}", mapName);
            Hazelcast.getMap(mapName);
        }

        
        // Initialize Jersey
	    
        ServletHolder jerseyHolder = new ServletHolder(ServletContainer.class);
        /*
        jerseyHolder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", 
                                      "com.sun.jersey.api.core.PackagesResourceConfig");
                                      */
        jerseyHolder.setInitParameter("com.sun.jersey.config.property.packages", 
                                      "com.mozilla.grouperfish.service");
        jerseyHolder.setInitOrder(1);
        

        // And Jetty
        
        int port = Integer.parseInt(System.getProperty("server.port", "8080"));
        Server server = new Server(port);
        ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        root.addServlet(jerseyHolder, "/*");
        
        server.setSendServerVersion(false);
        server.setSendDateHeader(false);
        server.setStopAtShutdown(true);

        server.start();
        server.join();
	}

}
