package com.mozilla.grouperfish.model;

public interface Access {

    enum Type {CREATE, READ, RUN, DELETE, LIST};

    String origin();

    Type type();

}
