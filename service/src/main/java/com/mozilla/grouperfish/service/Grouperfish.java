package com.mozilla.grouperfish.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.bagheera.rest.Bagheera;


/** Entry class to  set up the Grouperfish service. */
public class Grouperfish {

    public static final int DEFAULT_PORT = 0xF124;

    static Logger log = LoggerFactory.getLogger(Grouperfish.class);

    /**
     * Arguments:
     * - port number for the Grouperfish/Bagheera REST service
     * - HazelCast config file
     *
     * @param arguments
     * @throws Exception
     */
	public static void main(String[] arguments) throws Exception {

	    Bagheera.main(arguments);

	}

}
