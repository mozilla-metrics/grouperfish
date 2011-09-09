package com.mozilla.grouperfish.model;

import static com.mozilla.grouperfish.base.ImmutableTools.immutable;

import java.io.Serializable;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mozilla.grouperfish.base.Assert;


public abstract class NamedSource implements Serializable {

    private final String name;
    private final String source;
    private transient Map<String, ? extends Object> fields;

    NamedSource(final String name, final String source) {
        Assert.nonNull(name, source);
        Assert.check(!name.isEmpty(), !source.isEmpty());
        this.name = name;
        this.source = source;
    }

    /** @param fields Must be directly mappable to a JSONObject.
     *                That means, a java.util.Map with string keys and mappable values.
     *                http://code.google.com/p/json-simple/wiki/MappingBetweenJSONAndJavaEntities
     */
    NamedSource(final String name, final Map<String, ? extends Object> fields) {
        Assert.nonNull(name, fields);
        Assert.check(!name.isEmpty());
        this.name = name;
        this.fields = fields;
        this.source = JSONObject.toJSONString(fields);
    }

    public String toString() {
        return String.format("[%s %s, source.length=%s]", getClass().getSimpleName(), name(), source().length());
    }


    public String name() {
        return name;
    }

    public String source() {
        return source;
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
        Assert.check(fields instanceof Map);
        return fields;
    }

    private static final long serialVersionUID = 0;

}
