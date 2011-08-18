package com.mozilla.grouperfish.model;

import static com.mozilla.grouperfish.base.ImmutableTools.immutable;

import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mozilla.grouperfish.base.Assert;


public abstract class NamedSource {

    private final String name;
    private String source;
    private Map<String, ? extends Object> fields;


    NamedSource(final String name, final String source) {
        Assert.nonNull(name, source);
        this.name = name;
        this.source = source;
    }

    /** @param fields Must be directly mappable to a JSONObject.
     *                That means, a java.util.Map with string keys and mappable values.
     *                http://code.google.com/p/json-simple/wiki/MappingBetweenJSONAndJavaEntities
     */
    NamedSource(final String name, final Map<String, ? extends Object> fields) {
        Assert.nonNull(name, fields);
        this.name = name;
        this.fields = fields;
    }

    public String source() {
        if (source == null) source = JSONObject.toJSONString(fields);
        return source;
    }

    public String name() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public Map<String, ? extends Object> fields() {
        if (fields != null) return fields;
        try {
            fields = immutable((Map<String, ? extends Object>) new JSONParser().parse(source()));
        } catch (Exception e) {
            String message = String.format("Failed to parse source for %s with id='%s'",
                                           getClass().getSimpleName(), name);
            Assert.unreachable(message, e);
        }
        Assert.check(fields instanceof JSONObject);
        return fields;
    }

}
