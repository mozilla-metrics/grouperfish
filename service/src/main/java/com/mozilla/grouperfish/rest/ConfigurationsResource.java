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


@Path("/configurations/{namespace}")
public class ConfigurationsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFullConfiguration(@PathParam("namespace") String namespace,
                                         @Context HttpServletRequest request) {
        // :TODO: Next:
        // ... stream out all config entries ...
        return null;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putFullConfiguration(@PathParam("namespace") String namespace,
                                         @Context HttpServletRequest request) {
        // :TODO: Next:
        // ... replace all config entries ...
        return null;
    }


    @Path("/configurations/{namespace}/transforms/{name}")
    public static class TransformConfigsResource extends ResourceBase {

        @Inject
        public TransformConfigsResource(final Grid grid) { super(grid); }

        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        public Response put(@PathParam("namespace") String namespace,
                            @PathParam("name") String name,
                            @Context HttpServletRequest request) throws IOException {
            // :TODO: Next:
            // Validate parameters to match transform-specific schema.
            return RestHelper.putAny(getClass(), scope(namespace), name, request);
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response get(@PathParam("namespace") String namespace,
                            @PathParam("name") String name,
                            @Context HttpServletRequest request) {
            return RestHelper.getAny(getClass(), scope(namespace), name, request);
        }

        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response delete(@PathParam("namespace") String namespace,
                               @PathParam("name") String name,
                               @Context HttpServletRequest request) throws IOException {
            return RestHelper.deleteAny(getClass(), scope(namespace), name, request);
        }
    }


    @Path("/configurations/{namespace}/filters/{name}")
    public static class FilterConfigsResource extends ResourceBase {

        @Inject
        public FilterConfigsResource(final Grid grid) { super(grid); }

        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        public Response put(@PathParam("namespace") String namespace,
                            @PathParam("name") String name,
                            @Context HttpServletRequest request) throws IOException {
            // :TODO: Next:
            // 1. Validate parameters to match filter-specific schema.
            // 2. Implement some smart filter instantiation/registration so this has some effect.
            return RestHelper.putAny(getClass(), scope(namespace), name, request);
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response get(@PathParam("namespace") String namespace,
                            @PathParam("name") String name,
                            @Context HttpServletRequest request) {
            return RestHelper.getAny(getClass(), scope(namespace), name, request);
        }

        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public Response delete(@PathParam("namespace") String namespace,
                               @PathParam("name") String name,
                               @Context HttpServletRequest request) throws IOException {
            return RestHelper.deleteAny(getClass(), scope(namespace), name, request);
        }
    }


}
