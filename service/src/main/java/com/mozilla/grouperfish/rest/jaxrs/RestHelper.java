package com.mozilla.grouperfish.rest.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.StreamTool;
import com.mozilla.grouperfish.base.json.MapStreamer;
import com.mozilla.grouperfish.model.Access;
import com.mozilla.grouperfish.model.Access.Operation;
import com.mozilla.grouperfish.naming.Scope;


/** Bunch of internal helpers to cut down on resource specific code. */
class RestHelper {

    private static final Logger log = LoggerFactory.getLogger(RestHelper.class);


    /** Get any known resource. */
    static Response getAny(final Class<?> resourceType,
                           final Scope ns,
                           final String key,
                           final HttpServletRequest request) {

        if (!ns.allows(resourceType, new HttpAccess(Operation.READ, request))) return FORBIDDEN;

        final Map<String, String> map = ns.resourceMap(resourceType);
        final String source = map.get(key);
        if (source == null) {
            log.debug("404 Requested resource not found: {} {}", resourceType, ns.raw() + "::" + key);
            return NOT_FOUND;
        }

        return Response.ok(source, MediaType.APPLICATION_JSON).build();
    }


    /** Put any known resource. */
    static Response putAny(final Class<?> resourceType,
                           final Scope ns,
                           final String key,
                           final HttpServletRequest request) throws IOException {

        Access access = new HttpAccess(Operation.CREATE, request);
        if (!ns.allows(resourceType, access)) return FORBIDDEN;

        final int maxLength = ns.maxLength(resourceType, access);
        if (maxLength < request.getContentLength()) return TOO_LARGE;
        final String data = StreamTool.maybeConsume(request.getInputStream(), Charset.forName("UTF-8"), maxLength);
        if (data == null) return RestHelper.TOO_LARGE;

        if (!ns.validator(resourceType).isValid(data)) return BAD_REQUEST;

        final Map<String, String> map = ns.resourceMap(resourceType);
        map.put(key, data);

        return Response.created(URI.create(key)).build();
    }


    /**
     * You should only expose this API for small maps (queries, configuration).
     * @return a JSON map listing all named entities in this namespace and of this type.
     */
    static Response listAny(final Class<?> resourceType,
                            final Scope ns,
                            final HttpServletRequest request) {

        if (!ns.allows(resourceType, new HttpAccess(Operation.LIST, request))) return FORBIDDEN;

        final MapStreamer streamer = new MapStreamer(ns.resourceMap(resourceType));

        return Response.ok(
                new StreamingOutput() {
                    @Override
                    public void write(OutputStream output) throws IOException, WebApplicationException {
                        streamer.write(output);
                    }
                },
                MediaType.APPLICATION_JSON
            ).build();
    }

    /** Discard an item from the namespace. */
    static Response deleteAny(final Class<?> resourceType,
                              final Scope ns,
                              final String key,
                              final HttpServletRequest request) throws IOException {

        if (!ns.allows(resourceType, new HttpAccess(Operation.DELETE, request))) return FORBIDDEN;

        final Map<String, String> map = ns.resourceMap(resourceType);
        map.remove(key);

        return NO_CONTENT;
    }


    static final Response TOO_LARGE =
        Response.status(
            new StatusType() {
                public int getStatusCode() { return 413; }
                public Status.Family getFamily() { return Status.Family.CLIENT_ERROR; }
                public String getReasonPhrase() { return "Request Entity Too Large"; }
            }).build();

    static final Response FORBIDDEN =
        Response.status(Status.FORBIDDEN).build();

    static final Response BAD_REQUEST =
        Response.status(Status.BAD_REQUEST).build();

    static final Response NOT_FOUND =
        Response.status(Status.NOT_FOUND).build();

    static final Response ACCEPTED =
        Response.status(Status.ACCEPTED).build();

    static final Response FAIL =
        Response.serverError().build();

    static final Response NO_CONTENT =
        Response.noContent().build();
}
