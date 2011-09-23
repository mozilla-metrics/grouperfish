package com.mozilla.grouperfish.model;

import org.codehaus.jackson.map.ObjectMapper;

import com.mozilla.grouperfish.base.Assert;


/** Simple config+name wrapper. */
public class TransformConfig extends NamedSource {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final long serialVersionUID = 0;

    public TransformConfig(final String name, final String source) {
        super(name, source);
        Assert.nonNull(name, source);
    }

    public String parametersJson() {
        try {
            return mapper.writeValueAsString(fields().get("parameters"));
        } catch (final Exception e) {
            final String message = String.format(
                    "Failed to encode parameters as JSON for %s with name='%s'", getClass().getSimpleName(), name());
            throw new IllegalArgumentException(message, e);
        }
    }

    public String transform() {
        return fields().get("transform").toString();
    }

}
