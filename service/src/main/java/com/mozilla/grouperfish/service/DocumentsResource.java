package com.mozilla.grouperfish.service;

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

@Path("/documents/{namespace}/{id}")
public class DocumentsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocument(@PathParam("namespace") String namespace,
                                @PathParam("id") String id,
                                @Context HttpServletRequest request) {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.getAny(getClass(), ns, id, request);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putDocument(@PathParam("namespace") String namespace,
                                @PathParam("id") String id,
                                @Context HttpServletRequest request) throws IOException {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.putAny(getClass(), ns, id, request);
    }

    @DELETE
    public Response deleteDocument(@PathParam("namespace") String namespace,
                                   @PathParam("id") String id,
                                   @Context HttpServletRequest request) throws IOException {
        final Namespace ns = Namespace.get(namespace);
        return RestHelper.deleteAny(getClass(), ns, id, request);
    }

}
