package com.mozilla.grouperfish.rest;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Access;


public class HttpAccess implements Access {

    private final Access.Type type;
    private final HttpServletRequest request;

    @SuppressWarnings("serial")
    private static final Map<String, Access.Type> defaultType = new HashMap<String, Access.Type>() {{
        put("PUT", Access.Type.CREATE);
        put("GET", Access.Type.READ);
        put("POST", Access.Type.RUN);
        put("DELETE", Access.Type.DELETE);
    }};

    public HttpAccess(final HttpServletRequest request) {
        this(defaultType.get(request.getMethod()), request);
    }

    public HttpAccess(final Type type,
                      final HttpServletRequest request) {
        Assert.nonNull(type);
        this.type = type;
        this.request = request;
    }

    @Override
    public String origin() {
        return request.getRemoteHost();
    }

    @Override
    public Type type() {
        return type;
    }

}
