package com.mozilla.grouperfish.model;

import com.mozilla.grouperfish.base.Assert;


/** Simple config+name wrapper. */
public class TransformConfig extends NamedSource {

    public TransformConfig(final String name, final String source) {
        super(name, source);
        Assert.nonNull(name, source);
    }

}
