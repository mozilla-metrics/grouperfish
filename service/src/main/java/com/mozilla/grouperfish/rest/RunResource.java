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

import com.google.inject.Inject;
import com.mozilla.grouperfish.base.Box;
import com.mozilla.grouperfish.batch.BatchSystem;
import com.mozilla.grouperfish.model.ConfigurationType;
import com.mozilla.grouperfish.model.Access.Type;
import com.mozilla.grouperfish.model.Query;
import com.mozilla.grouperfish.model.TransformConfig;
import com.mozilla.grouperfish.naming.Scope;
import com.mozilla.grouperfish.services.Grid;

import static com.mozilla.grouperfish.rest.RestHelper.ACCEPTED;
import static com.mozilla.grouperfish.rest.RestHelper.FAIL;
import static com.mozilla.grouperfish.rest.RestHelper.FORBIDDEN;


public class RunResource {

    private static Logger log = LoggerFactory.getLogger(RunResource.class);


    @Path("/run/{namespace}")
    public static class ForAll extends RunResourceBase {

        @Inject
        public ForAll(final Grid grid, final BatchSystem system) { super(grid, system); }

        @POST
        public Response runTransformsForQuery(@PathParam("namespace") final String namespace,
                                              @Context final HttpServletRequest request) {
            final Scope ns = scope(namespace);

            if (!ns.allows(RunResource.class, new HttpAccess(Type.RUN, request))) {
                return FORBIDDEN;
            }

            try {
                batchSystem().schedule(ns);
            }
            catch (final Exception e) {
                log.error("Error initiating run request '{}': {}", request.getPathInfo(), e);
                return FAIL;
            }

            return ACCEPTED;
        }
    }


    @Path("/run/{namespace}/{queryName}")
    public static class ForQuery extends RunResourceBase {

        @Inject
        public ForQuery(final Grid grid, final BatchSystem system) { super(grid, system); }

        @POST
        public Response runTransformsForQuery(@PathParam("namespace") final String namespace,
                                              @PathParam("transformName") final String transformName,
                                              @PathParam("queryName") final String queryName,
                                              @Context final HttpServletRequest request) {
            final Scope ns = scope(namespace);

            if (!ns.allows(RunResource.class, new HttpAccess(Type.RUN, request))) {
                return FORBIDDEN;
            }

            final Box<Response> any404 = new Box<Response>();
            final Query q = fetchQuery(ns, queryName, any404);
            for (final Response some404 : any404) return some404;

            try {
                batchSystem().schedule(ns, q);
            }
            catch (final Exception e) {
                log.error("Error initiating run request '{}': {}", request.getPathInfo(), e);
                return FAIL;
            }

            return ACCEPTED;
        }

    }

    @Path("/run/{namespace}/{transformName}/{queryName}")
    public static class ForQueryWithTransform extends RunResourceBase {

        @Inject
        public ForQueryWithTransform(final Grid grid, final BatchSystem system) { super(grid, system); }

        public Response runOneTransformForQuery(@PathParam("namespace") final String namespace,
                                                @PathParam("transformName") final String transformName,
                                                @PathParam("queryName") final String queryName,
                                                @Context final HttpServletRequest request) {
            final Scope ns = scope(namespace);

            if (!ns.allows(RunResource.class, new HttpAccess(Type.RUN, request))) {
                return FORBIDDEN;
            }

            final Box<Response> any404 = new Box<Response>();

            final Query q = fetchQuery(ns, queryName, any404);
            for (final Response some404 : any404) return some404;

            final TransformConfig config = fetchTransformConfig(ns, transformName, any404);
            for (final Response some404 : any404) return some404;

            try {
                batchSystem().schedule(ns, q, config);
            }
            catch (final Exception e) {
                log.error("Error initiating run request '{}': {}", request.getPathInfo(), e);
                return FAIL;
            }

            return ACCEPTED;
        }

    }


    private static final Query fetchQuery(final Scope ns, final String name, final Box<Response> failure) {
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


    private static final TransformConfig fetchTransformConfig(final Scope ns,
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


    private static abstract class RunResourceBase extends ResourceBase {

        private final BatchSystem batchSystem;

        RunResourceBase(final Grid grid, final BatchSystem batchSystem) {
            super(grid);
            this.batchSystem = batchSystem;
        }

        protected BatchSystem batchSystem() {
            return batchSystem;
        }
    }

}
