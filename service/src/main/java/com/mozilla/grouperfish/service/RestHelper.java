package com.mozilla.grouperfish.service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import com.mozilla.grouperfish.base.StreamTool;
import com.mozilla.grouperfish.json.MapStreamer;
import com.mozilla.grouperfish.model.Namespace;


/** Bunch of internal helpers to cut down on resource specific code. */
// :TODO: Unit Test
class RestHelper {

    /** Get any known resource. */
    static Response getAny(final Class<?> resourceType,
                           final Namespace ns,
                           final String key,
                           final HttpServletRequest request) {

        if (!ns.allows(resourceType, "GET", request)) return FORBIDDEN;

        final Map<String, String> map = ns.resourceMap(resourceType);
        if (!map.containsKey(key)) return NOT_FOUND;

        return Response.ok(map.get(key), MediaType.APPLICATION_JSON).build();
    }


    /** Put any known resource. */
    static Response putAny(final Class<?> resourceType,
                           final Namespace ns,
                           final String key,
                           final HttpServletRequest request) throws IOException {

        if (!ns.allows(resourceType, "PUT", request)) return FORBIDDEN;

        final int maxLength = ns.maxLength(resourceType, request);
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
                            final Namespace ns,
                            final HttpServletRequest request) {

        if (!ns.allows(resourceType, "LIST", request)) return FORBIDDEN;

        final Map<String, String> map = ns.resourceMap(resourceType);
        return Response.ok(new MapStreamer(map), MediaType.APPLICATION_JSON).build();

    }

    /** Discard an item from the namespace. */
    static Response deleteAny(final Class<?> resourceType,
                              final Namespace ns,
                              final String key,
                              final HttpServletRequest request) throws IOException {

        if (!ns.allows(resourceType, "DELETE", request)) return FORBIDDEN;

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

    static final Response NO_CONTENT =
        Response.noContent().build();
}
