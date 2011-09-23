package com.mozilla.grouperfish.model;

import static com.mozilla.grouperfish.base.ImmutableTools.immutable;

import java.io.Serializable;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.mozilla.grouperfish.base.Assert;


public abstract class NamedSource implements Serializable {

    private final String name;
    private final String source;
    private transient Map<String, ? extends Object> fields;

    private static final TypeReference<Map<String, ? extends Object>> fieldsType =
            new TypeReference<Map<String, ? extends Object>>() {};

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final long serialVersionUID = 0;


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
        try {
            this.source = mapper.writeValueAsString(fields);
        }
        catch (final Exception e) {
            final String message = String.format(
                    "Failed to encode fields as JSON for %s with name='%s'", getClass().getSimpleName(), name);
            throw new IllegalArgumentException(message, e);
        }
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

    public Map<String, ? extends Object> fields() {
        if (fields != null) return fields;
        try {
            final Map<String, ? extends Object> map = mapper.readValue(source, fieldsType);
            fields = immutable(map);
        }
        catch (final Exception e) {
            final String message = String.format(
                    "Failed to parse source for %s with name='%s'", getClass().getSimpleName(), name);
            throw new IllegalArgumentException(message, e);
        }
        Assert.check(fields instanceof Map);
        return fields;
    }

}
