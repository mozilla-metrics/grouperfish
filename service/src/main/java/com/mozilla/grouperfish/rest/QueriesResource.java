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

@Path("/queries/{namespace}")
public class QueriesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("namespace") String namespace,
                         @Context HttpServletRequest request) {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.listAny(getClass(), ns, request);
    }

    @GET
    @Path("/{queryName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQuery(@PathParam("namespace") String namespace,
                             @PathParam("queryName") String queryName,
                             @Context HttpServletRequest request) {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.getAny(getClass(), ns, queryName, request);
    }

    @PUT
    @Path("/{queryName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putQuery(@PathParam("namespace") String namespace,
                             @PathParam("queryName") String queryName,
                             @Context HttpServletRequest request) throws IOException {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.putAny(getClass(), ns, queryName, request);
    }


    @DELETE
    @Path("/{queryName}")
    public Response deleteDocument(@PathParam("namespace") String namespace,
                                   @PathParam("queryName") String queryName,
                                   @Context HttpServletRequest request) throws IOException {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.deleteAny(getClass(), ns, queryName, request);
    }

}
