package com.mozilla.grouperfish.model;

public interface Access {

    enum Operation {CREATE, READ, RUN, DELETE, LIST};

    String origin();

    Operation type();

}
