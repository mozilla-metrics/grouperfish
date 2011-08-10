package com.mozilla.grouperfish.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/documents")
public class Documents {

    @GET
    @Produces("text/plain")
    public String get() {
        return "hello Jersey plus Jetty!";
    }
    
}
