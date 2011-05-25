package com.mozilla.bagheera.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.mozilla.bagheera.rest.ResourceBase;

@Path("/collections")
public class CollectionResource extends ResourceBase {

	public CollectionResource() throws IOException {
		super();
	}

	@POST
	@Path("{name}/{key}")
	public Response addDocument(@PathParam("name") String name, @PathParam("key") String key, @Context HttpServletRequest request) {
		// TODO: Trigger adding document to clusters
		return Response.noContent().build();
	}
	
}
