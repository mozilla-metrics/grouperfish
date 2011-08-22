package com.mozilla.grouperfish.model;

import java.util.Map;

import org.json.simple.JSONObject;

import com.mozilla.grouperfish.base.Assert;


/** Simple config+name wrapper. */
public class TransformConfig extends NamedSource {

    public TransformConfig(final String name, final String source) {
        super(name, source);
        Assert.nonNull(name, source);
    }

    private static final long serialVersionUID = 0;

    @SuppressWarnings("rawtypes")
    public String parametersJson() {
        return JSONObject.toJSONString((Map) fields().get("parameters"));
    }

    public String transform() {
        return (String) fields().get("transform");
    }

}
