package com.mozilla.grouperfish.base;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Can be used as an out-param or optional return value.
 * Either use has+get or iterate over the results.
 */
public class Box<T> implements Iterable<T> {

    private T value;

    public Box<T> put(final T value) {
        this.value = value;
        return this;
    }

    public boolean empty() {
        return value == null;
    }

    public T get() {
        return value;
    }

    /** Iterates 0 or 1 times. */
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean taken = false;

            @Override public boolean hasNext() {
                return !taken && !empty();
            }

            @Override public T next() {
                if (empty() || taken) throw new NoSuchElementException();
                taken = true;
                return value;
            }

            @Override public void remove() {
                if (empty() || taken) throw new NoSuchElementException();
                taken = true;
                value = null;
            }
        };

    }

}
