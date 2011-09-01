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


@Path("/documents/{namespace}/{id}")
public class DocumentsResource extends ResourceBase {

    @Inject
    public DocumentsResource(final Grid grid) { super(grid); }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocument(@PathParam("namespace") String namespace,
                                @PathParam("id") String id,
                                @Context HttpServletRequest request) {
        return RestHelper.getAny(getClass(), scope(namespace), id, request);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putDocument(@PathParam("namespace") String namespace,
                                @PathParam("id") String id,
                                @Context HttpServletRequest request) throws IOException {
        return RestHelper.putAny(getClass(), scope(namespace), id, request);
    }

    @DELETE
    public Response deleteDocument(@PathParam("namespace") String namespace,
                                   @PathParam("id") String id,
                                   @Context HttpServletRequest request) throws IOException {
        return RestHelper.deleteAny(getClass(), scope(namespace), id, request);
    }

}
