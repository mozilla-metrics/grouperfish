package com.mozilla.grouperfish.model;

import com.mozilla.grouperfish.base.Assert;


/** Simple query+name wrapper. */
public class Query extends NamedSource {

    public Query(final String name, final String json) {
        super(name, json);
        Assert.check(!name.isEmpty(), !json.isEmpty());
    }

    public boolean isTemplate() {
        // :TODO: NEXT:
        // Implement templates
        return false;
    }


}
