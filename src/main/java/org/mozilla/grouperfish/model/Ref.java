package org.mozilla.grouperfish.model;

public interface Ref<T extends Model> {
    Class<T> model();
}
