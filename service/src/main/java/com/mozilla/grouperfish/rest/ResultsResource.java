package com.mozilla.grouperfish.rest;

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

import com.mozilla.grouperfish.model.Namespace;

//:TODO: v0.1
// Integrate facet query parameters
@Path("/results/{namespace}/{transform}/{query}")
public class ResultsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResult(@PathParam("namespace") String namespace,
                              @PathParam("transform") String transformName,
                              @PathParam("query") String queryName,
                              @Context HttpServletRequest request) {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.getAny(getClass(), ns, key(transformName, queryName), request);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putResult(@PathParam("namespace") String namespace,
                              @PathParam("transform") String transformName,
                              @PathParam("query") String queryName,
                              @Context HttpServletRequest request) throws IOException {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.putAny(getClass(), ns, key(transformName, queryName), request);
    }

    @DELETE
    public Response deleteDocument(@PathParam("namespace") String namespace,
                                   @PathParam("transform") String transformName,
                                   @PathParam("query") String queryName,
                                   @Context HttpServletRequest request) throws IOException {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.deleteAny(getClass(), ns, key(transformName, queryName), request);
    }

    private String key(String transformName, String queryName) {
        return String.format("%s/%s", transformName, queryName);
    }

}
