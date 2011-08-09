package com.mozilla.grouperfish.service;

import org.apache.log4j.Logger;

import com.hazelcast.core.Hazelcast;
import com.mozilla.bagheera.rest.Bagheera;


/** Entry class to  set up the Grouperfish service. */
public class Grouperfish {
    
    static Logger log = Logger.getLogger(Grouperfish.class);

    /**
     * 
     * @param args
     * @throws Exception 
     */
	public static void main(String[] args) {
	    
	    Hazelcast.getMap("MYMAP");
	    
	    try {
	        Bagheera.main(args);
	    } 
	    catch (Exception e) {
	        log.error("Bagheera startup failed!");
	    }
	}

}
