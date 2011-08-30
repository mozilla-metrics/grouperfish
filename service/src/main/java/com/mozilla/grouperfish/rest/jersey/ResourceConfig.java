package com.mozilla.grouperfish.rest.jersey;

import com.sun.jersey.api.core.PackagesResourceConfig;

public class ResourceConfig extends PackagesResourceConfig {

    public ResourceConfig() {
        super("com.mozilla.grouperfish.rest.jaxrs");
    }

}
