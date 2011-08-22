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

import com.google.inject.Inject;
import com.mozilla.grouperfish.services.Grid;


@Path("/queries/{namespace}")
public class QueriesResource extends ResourceBase {

    @Inject
    public QueriesResource(final Grid grid) { super(grid); }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("namespace") String namespace,
                         @Context HttpServletRequest request) {
        return RestHelper.listAny(getClass(), scope(namespace), request);
    }

    @GET
    @Path("/{queryName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQuery(@PathParam("namespace") String namespace,
                             @PathParam("queryName") String queryName,
                             @Context HttpServletRequest request) {
        return RestHelper.getAny(getClass(), scope(namespace), queryName, request);
    }

    @PUT
    @Path("/{queryName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putQuery(@PathParam("namespace") String namespace,
                             @PathParam("queryName") String queryName,
                             @Context HttpServletRequest request) throws IOException {
        return RestHelper.putAny(getClass(), scope(namespace), queryName, request);
    }


    @DELETE
    @Path("/{queryName}")
    public Response deleteQuery(@PathParam("namespace") String namespace,
                                   @PathParam("queryName") String queryName,
                                   @Context HttpServletRequest request) throws IOException {
        return RestHelper.deleteAny(getClass(), scope(namespace), queryName, request);
    }

}
