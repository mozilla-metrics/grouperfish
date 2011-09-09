package com.mozilla.grouperfish.rest.jaxrs;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Access;


public class HttpAccess implements Access {

    private final Access.Operation type;
    private final HttpServletRequest request;

    @SuppressWarnings("serial")
    private static final Map<String, Access.Operation> defaultType = new HashMap<String, Access.Operation>() {{
        put("PUT", Access.Operation.CREATE);
        put("GET", Access.Operation.READ);
        put("POST", Access.Operation.RUN);
        put("DELETE", Access.Operation.DELETE);
    }};

    public HttpAccess(final HttpServletRequest request) {
        this(defaultType.get(request.getMethod()), request);
    }

    public HttpAccess(final Operation type,
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
    public Operation type() {
        return type;
    }

}
