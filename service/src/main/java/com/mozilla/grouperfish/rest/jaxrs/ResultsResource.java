package com.mozilla.grouperfish.rest.jaxrs;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.mozilla.grouperfish.services.api.Grid;


//:TODO: v0.1
// Integrate facet query parameters
@Path("/results/{namespace}/{transform}/{query}")
public class ResultsResource extends ResourceBase {

    @Inject
    public ResultsResource(final Grid grid) { super(grid); }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResult(@PathParam("namespace") String namespace,
                              @PathParam("transform") String transformName,
                              @PathParam("query") String queryName,
                              @Context HttpServletRequest request) {
        return RestHelper.getAny(getClass(), scope(namespace), key(transformName, queryName), request);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putResult(@PathParam("namespace") String namespace,
                              @PathParam("transform") String transformName,
                              @PathParam("query") String queryName,
                              @Context HttpServletRequest request) throws IOException {
        return RestHelper.putAny(getClass(), scope(namespace), key(transformName, queryName), request);
    }

    @DELETE
    public Response deleteResult(@PathParam("namespace") String namespace,
                                 @PathParam("transform") String transformName,
                                 @PathParam("query") String queryName,
                                 @Context HttpServletRequest request) throws IOException {
        return RestHelper.deleteAny(getClass(), scope(namespace), key(transformName, queryName), request);
    }

    public static String key(final String transformName, final String queryName) {
        return String.format("%s/%s", transformName, queryName);
    }

}
