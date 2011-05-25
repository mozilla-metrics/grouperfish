package com.mozilla.bagheera.rest;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.mozilla.bagheera.rest.ResourceBase;

@Path("/clusters")
public class ClusterResource extends ResourceBase {

	public ClusterResource() throws IOException {
		super();
	}

	@GET
	@Path("{name}/{key}")
	public String getCluster(@PathParam("name") String name, @PathParam("key") String key) {
		return "TBD";
	}
	
	@GET
	@Path("{name}/{key}/{label}")
	public String getClusters(@PathParam("name") String name, @PathParam("key") String key, @PathParam("label") String label) {
		return "TBD";
	}
	
}
