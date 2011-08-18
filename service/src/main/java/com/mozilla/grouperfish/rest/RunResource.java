package com.mozilla.grouperfish.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Box;
import com.mozilla.grouperfish.model.ConfigurationType;
import com.mozilla.grouperfish.model.Access.Type;
import com.mozilla.grouperfish.model.Namespace;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.model.TransformConfig;

import static com.mozilla.grouperfish.rest.RestHelper.ACCEPTED;
import static com.mozilla.grouperfish.rest.RestHelper.FAIL;
import static com.mozilla.grouperfish.rest.RestHelper.FORBIDDEN;


public class RunResource {

    private static Logger log = LoggerFactory.getLogger(RunResource.class);

    @Path("/run/{namespace}")
    public static class ForAll {

        @POST
        public Response runTransformsForQuery(@PathParam("namespace") final String namespace,
                                              @Context final HttpServletRequest request) {
            final Namespace ns = Namespace.get(namespace);

            if (!ns.allows(RunResource.class, new HttpAccess(Type.RUN, request))) {
                return FORBIDDEN;
            }

            try {
                ns.batchStarter().schedule();
            }
            catch (final Exception e) {
                log.error("Error initiating run request '{}': {}", request.getPathInfo(), e);
                return FAIL;
            }

            return ACCEPTED;
        }
    }


    @Path("/run/{namespace}/{queryName}")
    public static class ForQuery {

        @POST
        public Response runTransformsForQuery(@PathParam("namespace") final String namespace,
                                              @PathParam("transformName") final String transformName,
                                              @PathParam("queryName") final String queryName,
                                              @Context final HttpServletRequest request) {
            final Namespace ns = Namespace.get(namespace);

            if (!ns.allows(RunResource.class, new HttpAccess(Type.RUN, request))) {
                return FORBIDDEN;
            }

            final Box<Response> any404 = new Box<Response>();
            final Query q = fetchQuery(ns, queryName, any404);
            for (final Response some404 : any404) return some404;

            try {
                ns.batchStarter().schedule(q);
            }
            catch (final Exception e) {
                log.error("Error initiating run request '{}': {}", request.getPathInfo(), e);
                return FAIL;
            }

            return ACCEPTED;
        }

    }

    @Path("/run/{namespace}/{transformName}/{queryName}")
    public static class ForQueryWithTransform {

        @POST
        public Response runOneTransformForQuery(@PathParam("namespace") final String namespace,
                                                @PathParam("transformName") final String transformName,
                                                @PathParam("queryName") final String queryName,
                                                @Context final HttpServletRequest request) {
            final Namespace ns = Namespace.get(namespace);

            if (!ns.allows(RunResource.class, new HttpAccess(Type.RUN, request))) {
                return FORBIDDEN;
            }

            final Box<Response> any404 = new Box<Response>();

            final Query q = fetchQuery(ns, queryName, any404);
            for (final Response some404 : any404) return some404;

            final TransformConfig config = fetchTransformConfig(ns, transformName, any404);
            for (final Response some404 : any404) return some404;

            try {
                ns.batchStarter().schedule(q, config);
            }
            catch (final Exception e) {
                log.error("Error initiating run request '{}': {}", request.getPathInfo(), e);
                return FAIL;
            }

            return ACCEPTED;
        }

    }


    private static final Query fetchQuery(final Namespace ns, final String name, final Box<Response> failure) {
        final String json = ns.queries().get(name);
        if (json != null) {
            return new Query(name, json);
        }

        failure.put(Response.status(new StatusType() {
            @Override public int getStatusCode() { return 404; }
            @Override public String getReasonPhrase() { return "Unknown query: " + name; }
            @Override public Family getFamily() { return Family.CLIENT_ERROR; }
        }).build());
        return null;
    }


    private static final TransformConfig fetchTransformConfig(final Namespace ns,
                                                    final String name,
                                                    final Box<Response> failure) {
        final String json = ns.configurations(ConfigurationType.TRANSFOMS).get(name);
        if (json != null) {
            return new TransformConfig(name, json);
        }

        failure.put(Response.status(new StatusType() {
            @Override public int getStatusCode() { return 404; }
            @Override public String getReasonPhrase() { return "Unknown transform: " + name; }
            @Override public Family getFamily() { return Family.CLIENT_ERROR; }
        }).build());

        return null;
    }

}
